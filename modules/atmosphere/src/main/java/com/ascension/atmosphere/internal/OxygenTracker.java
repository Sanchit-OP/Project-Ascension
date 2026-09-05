package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.api.DrainModifier;
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
        if (!atRisk && state.suffocationTicks() == 0) {
            state.setDrainCarry(0.0f);
            suppressVanillaAir(player, atmosphere);

            OxygenSyncPayload last = state.lastSynced();
            boolean clientNeedsCorrecting = last == null || !last.breathable() || last.suffocating();
            boolean reconcileDue =
                    tick - state.lastSyncTick() >= AtmosphereTuning.RECONCILE_INTERVAL_TICKS;
            if (!clientNeedsCorrecting && !reconcileDue) {
                return;
            }
        }

        // One walk of the player's sources, reused for the failure check and the payload.
        // Previously available and capacity were gathered separately, walking every collector
        // twice more than necessary on every pass.
        Supply supply = gatherSupply(player, state);

        if (atRisk) {
            spend(player, state, drainPerSecond);
            supply = gatherSupply(player, state);
            applyFailure(player, state, supply);
        } else {
            state.setSuffocationTicks(0);
            state.setDrainCarry(0.0f);
        }

        suppressVanillaAir(player, atmosphere);
        sync(player, state, atmosphere, drainPerSecond, supply, tick);
    }

    /**
     * A player's total oxygen, gathered in a single pass over their sources.
     *
     * <p>Deliberately a value carried between steps rather than recomputed: the failure check
     * and the sync payload both need it, and walking every registered collector twice per
     * player per half-second is exactly the kind of quiet waste ADR-0007 exists to prevent.
     */
    private record Supply(int available, int capacity) {
    }

    private static Supply gatherSupply(ServerPlayer player, OxygenState state) {
        List<OxygenSourceCollector> collectors = ProviderRegistry.get().oxygenCollectors();
        if (collectors.isEmpty()) {
            return new Supply(state.units(), AtmosphereTuning.TANK_CAPACITY);
        }
        SupplySum sum = new SupplySum();
        for (int i = 0; i < collectors.size(); i++) {
            collectors.get(i).collect(player, sum);
        }
        int capacity = sum.capacity > 0 ? sum.capacity : AtmosphereTuning.TANK_CAPACITY;
        return new Supply(state.units() + sum.available, capacity);
    }

    // --- consumption --------------------------------------------------------

    private static void spend(ServerPlayer player, OxygenState state, float drainPerSecond) {
        float owed = drainPerSecond * AtmosphereTuning.accountingSeconds() + state.drainCarry();
        int whole = (int) owed;
        state.setDrainCarry(owed - whole);
        if (whole <= 0) {
            return;
        }

        int remaining = drawFromSources(player, whole);
        if (remaining > 0) {
            // Fall back to the reserve held directly on the player. Until collectors exist
            // (M1.7), this is the only supply there is.
            int fromReserve = Math.min(remaining, state.units());
            state.setUnits(state.units() - fromReserve);
        }
    }

    /**
     * Draw from registered sources in {@code drawOrder}, lowest first.
     *
     * <p>Portable tanks sit below suit reserve, so the suit stays a safety margin and running a
     * tank dry is a warning rather than a death sentence.
     *
     * @return units still owed after every source was exhausted
     */
    private static int drawFromSources(ServerPlayer player, int units) {
        List<OxygenSourceCollector> collectors = ProviderRegistry.get().oxygenCollectors();
        if (collectors.isEmpty()) {
            return units;
        }
        List<OxygenSource> sources = new ArrayList<>();
        Consumer<OxygenSource> sink = sources::add;
        for (int i = 0; i < collectors.size(); i++) {
            collectors.get(i).collect(player, sink);
        }
        sources.sort(Comparator.comparingInt(OxygenSource::drawOrder));

        int owed = units;
        for (int i = 0; i < sources.size() && owed > 0; i++) {
            owed -= sources.get(i).consume(owed);
        }
        return owed;
    }

    // --- failure ------------------------------------------------------------

    private static void applyFailure(ServerPlayer player, OxygenState state, Supply supply) {
        if (supply.available() > 0) {
            state.setSuffocationTicks(0);
            return;
        }

        int ticks = state.suffocationTicks() + AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS;
        state.setSuffocationTicks(ticks);

        if (ticks <= AtmosphereTuning.SUFFOCATION_GRACE_TICKS) {
            // Grace window: the player is told, loudly, but not yet hurt.
            return;
        }

        float damage = AtmosphereTuning.SUFFOCATION_DAMAGE * AtmosphereTuning.accountingSeconds();
        player.hurt(player.damageSources().source(NO_OXYGEN), damage);
    }

    // --- vanilla air --------------------------------------------------------

    /**
     * Hold vanilla's air supply full while we are managing breathing.
     *
     * <p>Without this the player runs two timers at once and drowns on vanilla's schedule while
     * our bar still shows air. Deliberately skipped when the feature is disabled, so turning it
     * off restores stock behaviour exactly rather than leaving a half-converted state.
     */
    private static void suppressVanillaAir(ServerPlayer player, Atmosphere atmosphere) {
        if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
            return;
        }
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }

    // --- sync ---------------------------------------------------------------

    private static void sync(ServerPlayer player, OxygenState state, Atmosphere atmosphere,
                             float drainPerSecond, Supply supply, long tick) {
        OxygenSyncPayload payload = new OxygenSyncPayload(
                supply.available(),
                supply.capacity(),
                atmosphere.breathable(),
                drainPerSecond,
                state.suffocationTicks() > 0);

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

    /**
     * Accumulates available and capacity together in one pass.
     *
     * <p>An explicit class rather than captured {@code int[]} boxes, and one class rather than
     * two: the earlier version allocated a fresh box per collector, per player, per accounting
     * pass, inside exactly the loop ADR-0007 asks to keep quiet.
     */
    private static final class SupplySum implements Consumer<OxygenSource> {
        private int available;
        private int capacity;

        @Override
        public void accept(OxygenSource source) {
            available += source.available();
            capacity += source.capacity();
        }
    }

    /** Force a resend on the next pass, e.g. after a dimension change or respawn. */
    public static void invalidate(ServerPlayer player) {
        player.getData(AtmosphereAttachments.OXYGEN).invalidateSync();
    }
}
