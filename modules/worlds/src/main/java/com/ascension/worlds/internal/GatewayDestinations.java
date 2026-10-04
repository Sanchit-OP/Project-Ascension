package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Where an already-active gateway portal sends a player who uses it: a clickable list of every
 * other planet whose own portal is active, in chat.
 *
 * <p><strong>A placeholder, the same way {@code OxygenRefillStationBlock}'s instant, free fill
 * is.</strong> A real destination-picker screen is new surface area &mdash; {@code Screen} +
 * {@code AbstractContainerMenu} + a {@code MenuType} + client registration &mdash; that this
 * project has never needed before, and {@code plans/m3-vertical-slice.md} flagged it as exactly
 * that: worth its own focused pass once the underlying network (activation, shared state, instant
 * travel) is proven live, not before. A clickable chat message exercises that whole underlying
 * mechanism today with zero new client-side plumbing.
 *
 * <p>Each line's {@link ClickEvent} runs {@code /ascension worlds gateway travel <planet>} &mdash;
 * see {@link WorldsCommands} &mdash; rather than this class calling {@link SpaceMechanics}
 * directly, because a chat click is exactly a player-issued command from the server's point of
 * view, and going through the same command the picker could also be typed by hand keeps there
 * being exactly one entry point to "teleport me to this planet's surface right now."
 */
final class GatewayDestinations {

    private GatewayDestinations() {
    }

    static void sendPicker(ServerPlayer player, ServerLevel level) {
        GatewayNetworkState network = level.getServer().overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
        Registry<Planet> planets = level.getServer().registryAccess().registryOrThrow(WorldsRegistries.PLANET);

        List<Map.Entry<ResourceKey<Planet>, Planet>> reachable = new ArrayList<>();
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : planets.entrySet()) {
            Planet planet = entry.getValue();
            if (planet.surface().equals(level.dimension())) {
                continue;
            }
            if (network.isActive(planet.surface().location())) {
                reachable.add(entry);
            }
        }

        if (reachable.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("block.ascension_worlds.gateway_portal.no_destinations")
                            .withStyle(ChatFormatting.GRAY),
                    false);
            return;
        }

        player.displayClientMessage(
                Component.translatable("block.ascension_worlds.gateway_portal.pick_destination")
                        .withStyle(ChatFormatting.AQUA),
                false);
        for (Map.Entry<ResourceKey<Planet>, Planet> entry : reachable) {
            ResourceKey<Planet> planetKey = entry.getKey();
            String dimensionId = entry.getValue().surface().location().toString();
            MutableComponent line = Component.literal("  > ")
                    .append(Component.translatable("planet." + planetKey.location().getNamespace()
                            + "." + planetKey.location().getPath()))
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/ascension worlds gateway travel " + dimensionId))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Travel to " + dimensionId))));
            player.displayClientMessage(line, false);
        }
    }
}
