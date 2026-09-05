package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.api.DrainModifier;
import com.ascension.atmosphere.api.LungCapacityModifier;
import com.ascension.atmosphere.api.OxygenSource;
import com.ascension.atmosphere.api.OxygenSourceCollector;
import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side accounting: works out each player's oxygen situation, spends it, and syncs it.
 *
 * <p>Runs every {@link AtmosphereTuning#ACCOUNTING_INTERVAL_TICKS} ticks over the online player
 * list. No world scan, no per-block work: cost is proportional to players online, not world
 * size (ADR-0007 rule 4).
 *
 * <p>Holds no state of its own. Everything per-player lives on that player's attachment, so it
 * cannot outlive them.
 */
public final class OxygenTracker {

    /** Damage dealt when the failure window elapses. Defined as data, not a vanilla stand-in. */
    public static final ResourceKey<DamageType> NO_OXYGEN = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "no_oxygen"));

    private OxygenTracker() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS != 0) {
            return;
        }
        long tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player, tick);
        }
    }

    private static void update(ServerPlayer player, long tick) {
        OxygenState state = player.getData(AtmosphereAttachments.OXYGEN);

        // Before anything else, and deliberately outside the fast path below: a tank opened in
        // breathable air must still come up to pressure while you walk to the airlock. Making
        // the timer run only when you are already in danger would mean it never ran when it
        // mattered and always ran when it hurt most.
        if (state.pressurisingTicks() > 0) {
            state.setPressurisingTicks(
                    state.pressurisingTicks() - AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS);
        }

        // Breathing happens at the head, not the feet. Standing chest-deep in water should not
        // suffocate you, and standing on a sealed floor with your head in vacuum should.
        Atmosphere atmosphere =
                AtmosphereRegistry.query(player.serverLevel(), player.getEyePosition());

        float drainPerSecond = drainPerSecond(player, atmosphere);
        boolean exempt = player.isCreative() || player.isSpectator();
        boolean atRisk = drainPerSecond > 0.0f && !exempt;

        // Fast path. A player breathing normally is the common case for the entire first act
        // of the campaign, and it should cost almost nothing: one atmosphere query and two
        // comparisons, with no source walking and no payload built.
        int lungCapacity = lungCapacity(player);

        // Gear can be removed mid-dive. Trim the reserve down rather than leaving a player
        // holding more air than their lungs can now contain.
        if (state.lungUnits() > lungCapacity) {
            state.setLungUnits(lungCapacity);
        }

        if (!atRisk && state.suffocationTicks() == 0 && state.lungUnits() >= lungCapacity
                && state.pressurisingTicks() == 0) {
            state.setDrainCarry(0.0f);
            OxygenSyncPayload last = state.lastSynced();
            boolean clientNeedsCorrecting =
                    last == null || !last.breathable() || last.suffocating() || last.refilling()
                            || last.pressurisingSeconds() > 0;
            boolean reconcileDue =
                    tick - state.lastSyncTick() >= AtmosphereTuning.RECONCILE_INTERVAL_TICKS;
            if (!clientNeedsCorrecting && !reconcileDue) {
                return;
            }
        }

        // Change the supply first, then read it once. The reading is what the failure check and
        // the payload both need, and it has to happen after the change or the client would
        // always be shown the previous pass's number.
        //
        // Gathering before the branch as well used to look harmless. It stopped being harmless
        // the moment a collector did real work: the tank collector walks 41 inventory slots, so
        // the discarded gather was a wasted inventory scan per player per pass.
        //
        // And the same mistake survived one layer down until the M2.4 pass. Spending drew from a
        // freshly gathered list and then the summary gathered a second one, so a player in danger
        // paid two full inventory walks every pass -- the exact waste the note on Supply below
        // claims to prevent. Gathered once here now, and used for both.
        List<OxygenSource> sources = gatherSources(player);

        Supply supply;
        if (atRisk) {
            spend(state, drainPerSecond, sources);
            supply = summarise(state, lungCapacity, sources);
            applyFailure(player, state, supply);
        } else {
            state.setSuffocationTicks(0);
            state.setDrainCarry(0.0f);
            refillLungs(state, lungCapacity);
            supply = summarise(state, lungCapacity, sources);
        }

        sync(player, state, atmosphere, drainPerSecond, supply, lungCapacity, tick);
    }

    /**
     * A player's total oxygen: lungs plus everything they are carrying.
     *
     * <p>Deliberately a value carried between steps rather than recomputed. The failure check and
     * the sync payload both need it, and walking every registered collector twice per player per
     * half-second is exactly the kind of quiet waste ADR-0007 exists to prevent.
     */
    public record Supply(int available, int capacity) {
    }

    /**
     * One-shot reading of everything a player can breathe from.
     *
     * <p>For commands and diagnostics. The accounting path deliberately does not call this: it
     * already knows the lung capacity it computed a few lines earlier and should not pay to work
     * it out twice.
     */
    public static Supply supply(ServerPlayer player) {
        return summarise(player.getData(AtmosphereAttachments.OXYGEN), lungCapacity(player),
                gatherSources(player));
    }

    /**
     * Everything a player can currently breathe from, asked for once.
     *
     * <p>This is the expensive step in the whole pass &mdash; a collector may walk an inventory,
     * a curio slot or a vehicle &mdash; so the caller gathers once and both spends from and sums
     * the same list.
     */
    private static List<OxygenSource> gatherSources(ServerPlayer player) {
        List<OxygenSourceCollector> collectors = ProviderRegistry.get().oxygenCollectors();
        if (collectors.isEmpty()) {
            return List.of();
        }
        List<OxygenSource> sources = new ArrayList<>(collectors.size());
        Consumer<OxygenSource> sink = sources::add;
        for (int i = 0; i < collectors.size(); i++) {
            collectors.get(i).collect(player, sink);
        }
        return sources;
    }

    /**
     * Add up already-gathered sources, plus the lungs that are always part of the total.
     *
     * <p>Reads the sources rather than a number captured when they were gathered, so calling this
     * after {@link #spend} reports what is left rather than what there was.
     */
    private static Supply summarise(OxygenState state, int lungCapacity,
                                    List<OxygenSource> sources) {
        int available = state.lungUnits();
        int capacity = lungCapacity;
        for (int i = 0; i < sources.size(); i++) {
            OxygenSource source = sources.get(i);
            available += source.available();
            capacity += source.capacity();
        }
        return new Supply(available, capacity);
    }

    /**
     * Base lungs plus whatever worn gear adds.
     *
     * <p>Gear grows the reserve rather than slowing consumption, so a helmet enchantment never
     * silently stretches the duration of a carried tank.
     */
    public static int lungCapacity(ServerPlayer player) {
        int capacity = AtmosphereTuning.LUNG_CAPACITY;
        List<LungCapacityModifier> modifiers = ProviderRegistry.get().lungCapacityModifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            capacity += Math.max(0, modifiers.get(i).bonusUnits(player));
        }
        return capacity;
    }

    /**
     * Top lungs back up in breathable air.
     *
     * <p>Without this a player who died of suffocation respawned with an empty reserve and
     * started suffocating again the moment they touched water — the bug that prompted the
     * lung/tank split. The arithmetic, and why it always gains at least one unit, is in
     * {@link OxygenAccounting#lungsAfterRefill}.
     */
    private static void refillLungs(OxygenState state, int lungCapacity) {
        state.setLungUnits(OxygenAccounting.lungsAfterRefill(state.lungUnits(), lungCapacity));
    }

    // --- consumption --------------------------------------------------------

    private static void spend(OxygenState state, float drainPerSecond,
                              List<OxygenSource> sources) {
        OxygenAccounting.Debt debt = OxygenAccounting.debt(drainPerSecond, state.drainCarry());
        state.setDrainCarry(debt.carry());
        if (debt.units() <= 0) {
            return;
        }

        // Tanks first, lungs last. Emptying a tank should be a warning that sends you back to
        // air, not the moment you start dying: the lung reserve is what buys you that trip.
        int remaining = drawFromSources(sources, debt.units());
        if (remaining > 0) {
            int fromLungs = Math.min(remaining, state.lungUnits());
            state.setLungUnits(state.lungUnits() - fromLungs);
        }
    }

    /**
     * Draw from the gathered sources in {@code drawOrder}, lowest first.
     *
     * <p>Portable tanks sit below suit reserve, so the suit stays a safety margin and running a
     * tank dry is a warning rather than a death sentence.
     *
     * <p>Sorts in place, and only when there is something to order. With one source &mdash; the
     * open tank, which is the whole of the game today &mdash; a sort is pure ceremony.
     *
     * @return units still owed after every source was exhausted
     */
    private static int drawFromSources(List<OxygenSource> sources, int units) {
        if (sources.size() > 1) {
            sources.sort(Comparator.comparingInt(OxygenSource::drawOrder));
        }
        int owed = units;
        for (int i = 0; i < sources.size() && owed > 0; i++) {
            owed -= sources.get(i).consume(owed);
        }
        return owed;
    }

    // --- failure ------------------------------------------------------------

    private static void applyFailure(ServerPlayer player, OxygenState state, Supply supply) {
        int ticks = OxygenAccounting.suffocationTicksAfter(
                state.suffocationTicks(), supply.available() > 0);
        state.setSuffocationTicks(ticks);

        // Inside the grace window the player is told, loudly, but not yet hurt.
        if (OxygenAccounting.damageDue(ticks)) {
            player.hurt(player.damageSources().source(NO_OXYGEN),
                    OxygenAccounting.damagePerPass());
        }
    }

    // --- sync ---------------------------------------------------------------

    private static void sync(ServerPlayer player, OxygenState state, Atmosphere atmosphere,
                             float drainPerSecond, Supply supply, int lungCapacity, long tick) {
        OxygenSyncPayload payload = new OxygenSyncPayload(
                supply.available(),
                supply.capacity(),
                atmosphere.breathable(),
                drainPerSecond,
                state.suffocationTicks() > 0,
                state.lungUnits() < lungCapacity,
                // Rounded up, so a countdown never shows "0s" while it is still running.
                Math.ceilDiv(state.pressurisingTicks(), 20));

        boolean changed = payload.differsFrom(state.lastSynced());
        boolean reconcileDue =
                tick - state.lastSyncTick() >= AtmosphereTuning.RECONCILE_INTERVAL_TICKS;

        if (changed || reconcileDue) {
            PacketDistributor.sendToPlayer(player, payload);
            state.recordSync(payload, tick);
        }
    }

    /**
     * Effective drain: where you are, times what you are doing, divided by what you are wearing.
     *
     * <p>Breathable air costs nothing, so the common case short-circuits before touching the
     * modifier list.
     */
    public static float drainPerSecond(ServerPlayer player, Atmosphere atmosphere) {
        if (atmosphere.breathable()) {
            return 0.0f;
        }
        float drain = AtmosphereTuning.BASE_UNITS_PER_SECOND * atmosphere.drainMultiplier();

        List<DrainModifier> modifiers = ProviderRegistry.get().drainModifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            drain *= modifiers.get(i).multiplier(player);
        }
        return Math.max(0.0f, drain);
    }

    /** Force a resend on the next pass, e.g. after a dimension change or respawn. */
    public static void invalidate(ServerPlayer player) {
        player.getData(AtmosphereAttachments.OXYGEN).invalidateSync();
    }
}
