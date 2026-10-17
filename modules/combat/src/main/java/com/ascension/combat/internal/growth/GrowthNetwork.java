package com.ascension.combat.internal.growth;

import com.ascension.combat.client.ClientGrowth;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Growth's own network traffic, registered apart from {@code CombatNetwork} with its own version
 * (NeoForge versions each payload, not each mod). Down: {@link GrowthSyncPayload}, the player's state,
 * from {@link PlayerGrowth#refresh}. Up: {@link GrowthTakePayload} and {@link GrowthRespecPayload}, the
 * tree screen's Confirm and Respec. The server owns every rule; a client can only ask.
 *
 * <p>The client's handler is named only on the client, as {@code CombatNetwork} does it, so a dedicated
 * server never loads a client class.
 */
final class GrowthNetwork {

    /** Bump when a payload's shape changes, so a mismatched client is refused at handshake. */
    private static final String VERSION = "1";

    private GrowthNetwork() {
    }

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(GrowthTakePayload.TYPE, GrowthTakePayload.STREAM_CODEC, GrowthNetwork::onTake);
        registrar.playToServer(GrowthRespecPayload.TYPE, GrowthRespecPayload.STREAM_CODEC, GrowthNetwork::onRespec);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(GrowthSyncPayload.TYPE, GrowthSyncPayload.STREAM_CODEC, ClientGrowth::onSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(GrowthSyncPayload.TYPE, GrowthSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
        }
    }

    /** Tells the player's own client where their growth stands. */
    static void sync(ServerPlayer player) {
        sync(player, PlayerGrowth.earnedMilestones(player));
    }

    /** As {@link #sync(ServerPlayer)}, for a caller that has just worked the earned milestones out. */
    static void sync(ServerPlayer player, List<GrowthMilestone> earned) {
        if (player.connection == null) {
            return;
        }
        List<ResourceLocation> milestones = new ArrayList<>();
        earned.forEach(milestone -> milestones.add(milestone.advancement()));
        PacketDistributor.sendToPlayer(player, new GrowthSyncPayload(
                List.copyOf(player.getData(GrowthAttachments.GROWTH).takenInOrder()),
                milestones,
                earned.stream().mapToInt(GrowthMilestone::points).sum(),
                PlayerGrowth.pointsSpent(player)));
    }

    private static void onTake(GrowthTakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerGrowth.TakeFailure failure = PlayerGrowth.takeAll(player, payload.nodes());
            if (failure != null) {
                // Whatever was taken has already been synced; this tells the screen what it still has
                // marked is not so, and says why.
                sync(player);
                player.displayClientMessage(PlayerGrowth.takeFailure(
                        failure.check(), PlayerGrowth.nodeName(player.server, failure.node())), true);
            }
        });
    }

    private static void onRespec(GrowthRespecPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GrowthRules.RespecCheck check = PlayerGrowth.respec(player);
            Component message = check == GrowthRules.RespecCheck.OK
                    ? Component.translatable("growth.ascension_combat.respec.done", PlayerGrowth.pointsAvailable(player))
                    : PlayerGrowth.respecFailure(check);
            player.displayClientMessage(message, true);
        });
    }
}
