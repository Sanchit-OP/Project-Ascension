package com.ascension.combat.client;

import com.ascension.combat.internal.growth.GrowthSyncPayload;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What this client knows of its own player's growth (docs/progression/growth.md section 9): the last
 * {@link GrowthSyncPayload}, kept whole and replaced whole, so the tree screen reads one consistent
 * picture and never half of a change. Ids and two numbers only: no player, level or entity is held
 * here (ADR-0007), and logging out clears it.
 */
public final class ClientGrowth {

    /**
     * @param taken nodes taken, in order
     * @param milestones advancement ids of the milestones earned
     * @param earned points earned
     * @param spent points spent
     */
    public record Snapshot(List<ResourceLocation> taken, List<ResourceLocation> milestones, int earned, int spent) {

        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), 0, 0);

        public int available() {
            return earned - spent;
        }
    }

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private ClientGrowth() {
    }

    public static Snapshot get() {
        return snapshot;
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ClientGrowth::onLoggingOut);
    }

    public static void onSync(GrowthSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> snapshot = new Snapshot(
                List.copyOf(payload.taken()), List.copyOf(payload.milestones()), payload.earned(), payload.spent()));
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        snapshot = Snapshot.EMPTY;
    }
}
