package com.ascension.worlds.internal;

import com.ascension.worlds.internal.net.GateProgressPayload;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Keeps every client told which gates are lit ({@link GateProgressPayload}): once when a player
 * logs in, and to everyone whenever the network changes. Event-driven; nothing here ticks.
 *
 * <p>The story's reading of the same number is the <em>mark</em> ({@code docs/lore/the-mark.md}):
 * every gate lit adds a glyph to the player's hand, which is the Observers counting the seal's
 * broken locks. {@link #markStage} is that count, and the pillar and the orrery will read it too.
 *
 * <p>The network is shared, not per player ({@link GatewayNetworkState}), so the stage is the
 * team's: everyone on the server wears the same mark, the way everyone shares the gates
 * (vision: team progression is shared at the team level). Teams proper belong to a progression
 * module that doesn't exist yet.
 */
public final class GateProgress {

    /** The mark has seven glyphs, one per gate on the road; there is no picture past the seventh. */
    public static final int MAX_STAGE = 7;

    private GateProgress() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GateProgress::onPlayerLoggedIn);
    }

    /**
     * How many glyphs the mark shows for {@code litGates} lit gates: every gate adds one, up to
     * {@link #MAX_STAGE}. Planets cannot be skipped on the critical path, so the n-th glyph is always
     * the n-th gate lit, and a count is all any reader needs.
     */
    public static int markStage(int litGates) {
        return Math.max(0, Math.min(MAX_STAGE, litGates));
    }

    /** Tells every connected client the network's current state. Call after any change to it. */
    public static void broadcast(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(payload(server));
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload(player.server));
        }
    }

    private static GateProgressPayload payload(MinecraftServer server) {
        List<ResourceLocation> lit = List.copyOf(
                server.overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE).activePlanets());
        return new GateProgressPayload(lit);
    }
}
