package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import com.ascension.worlds.internal.net.GatewayDestinationsPayload;
import com.ascension.worlds.internal.net.GatewayTravelPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Where an active gateway portal can send a player, and the one place that decides whether a
 * given trip is allowed.
 *
 * <p>Using an active portal sends the player the reachable destinations
 * ({@link GatewayDestinationsPayload}), which the client shows as a screen; picking one comes back
 * as a {@link GatewayTravelPayload}. The typed {@code /ascension worlds gateway travel} command is
 * a second way in. Both go through {@link #tryTravel}, so there is exactly one rule: a
 * non-operator needs a live {@link GatewayPickerSession} &mdash; opened only here, by an active
 * portal &mdash; and a destination {@link GatewayNetworkState#canTravel} allows. The client is
 * never trusted with any of it.
 */
public final class GatewayDestinations {

    private GatewayDestinations() {
    }

    static void sendPicker(ServerPlayer player, ServerLevel level) {
        GatewayNetworkState network = level.getServer().overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
        Registry<Planet> planets = level.getServer().registryAccess().registryOrThrow(WorldsRegistries.PLANET);
        ResourceLocation here = level.dimension().location();

        List<GatewayDestinationsPayload.Destination> reachable = new ArrayList<>();
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : planets.entrySet()) {
            ResourceLocation surface = entry.getValue().surface().location();
            if (network.canTravel(here, surface)) {
                reachable.add(new GatewayDestinationsPayload.Destination(entry.getKey().location(), surface));
            }
        }

        if (reachable.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("block.ascension_worlds.gateway_portal.no_destinations")
                            .withStyle(ChatFormatting.GRAY),
                    true);
            return;
        }

        player.getData(SpaceAttachments.GATEWAY_PICKER_SESSION).open(here, level.getGameTime());
        PacketDistributor.sendToPlayer(player, new GatewayDestinationsPayload(List.copyOf(reachable)));
    }

    /**
     * Move {@code player} to {@code surface} if the gateway rules allow it. Returns null on
     * success, or the reason it was refused. Operators skip the session check as a debug tool,
     * the same way {@code weather force} is operator-only.
     */
    @Nullable
    static Component tryTravel(ServerPlayer player, ResourceLocation surface, boolean operator) {
        ServerLevel level = player.serverLevel();
        ResourceLocation here = level.dimension().location();

        if (!operator) {
            GatewayPickerSession session = player.getData(SpaceAttachments.GATEWAY_PICKER_SESSION);
            GatewayNetworkState network = level.getServer().overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
            if (!session.isValidIn(here, level.getGameTime()) || !network.canTravel(here, surface)) {
                return Component.translatable("block.ascension_worlds.gateway_portal.use_a_portal");
            }
            session.close();
        }

        if (!SpaceMechanics.travelViaGateway(player, surface)) {
            return Component.literal(surface + " is not a planet surface.");
        }
        return null;
    }

    /** {@link GatewayTravelPayload} handler: a destination picked on the screen. */
    public static void onTravelRequest(GatewayTravelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Component refusal = tryTravel(player, payload.surface(), false);
            if (refusal != null) {
                player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
            }
        });
    }
}
