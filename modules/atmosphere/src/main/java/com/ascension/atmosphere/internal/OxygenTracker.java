package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.api.DrainModifier;
import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side accounting: works out each player's oxygen situation and syncs it.
 *
 * <p>Runs every {@link AtmosphereTuning#ACCOUNTING_INTERVAL_TICKS} ticks over the online player
 * list. There is no world scan and no per-block work here &mdash; the cost is proportional to
 * players online, not to world size (ADR-0007 rule 4).
 *
 * <p>Holds no state of its own. Everything per-player lives on that player's attachment, so it
 * cannot outlive them.
 *
 * <p>Consumption is deliberately not implemented yet; that is M1.5. This computes the drain
 * <em>rate</em> so the readout is honest, but does not yet subtract it.
 */
public final class OxygenTracker {

    private OxygenTracker() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS != 0) {
            return;
        }
        long tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player, tick);
        }
    }

    private static void syncPlayer(ServerPlayer player, long tick) {
        OxygenState state = player.getData(AtmosphereAttachments.OXYGEN);
        Atmosphere atmosphere = AtmosphereRegistry.query(player.serverLevel(), player.position());

        float drainPerSecond = drainPerSecond(player, atmosphere);

        OxygenSyncPayload payload = new OxygenSyncPayload(
                state.units(),
                capacityOf(player),
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
     * Effective drain, following the model in {@code docs/technical/atmosphere-api.md}:
     * where you are, times what you are doing, divided by what you are wearing.
     *
     * <p>Breathable air costs nothing at all, so the common case short-circuits before touching
     * the modifier list.
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
        return drain;
    }

    /**
     * Total capacity across the player's sources.
     *
     * <p>No collectors are registered in v0.1, so this reports the debug reserve held directly
     * on the attachment. Tanks and suit reserves arrive with M1.7 and {@code ascension-gear}.
     */
    private static int capacityOf(ServerPlayer player) {
        int collected = 0;
        for (var collector : ProviderRegistry.get().oxygenCollectors()) {
            final int[] sum = {0};
            collector.collect(player, source -> sum[0] += source.capacity());
            collected += sum[0];
        }
        return collected > 0 ? collected : AtmosphereTuning.TANK_CAPACITY;
    }

    /** Force a resend on the next pass, e.g. after a dimension change or respawn. */
    public static void invalidate(ServerPlayer player) {
        player.getData(AtmosphereAttachments.OXYGEN).invalidateSync();
    }
}
