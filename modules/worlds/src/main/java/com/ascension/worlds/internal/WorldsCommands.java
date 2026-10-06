package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.internal.net.PlanetWeatherSyncPayload;
import com.ascension.worlds.api.WorldsRegistries;
import com.ascension.worlds.internal.archive.ArchiveDebug;
import com.ascension.worlds.internal.archive.ArchiveEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Debug and diagnostic commands, mirroring {@code AtmosphereCommands}'s reasoning: a storm timer
 * ticking silently server-side is exactly the kind of state a bug report cannot describe
 * precisely without a way to read it directly ("I think it lasted 10 seconds" is not a
 * measurement). The one gameplay command, {@code gateway travel}, is guarded separately &mdash; see
 * {@code gatewayTravel}.
 */
public final class WorldsCommands {

    private WorldsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascension")
                .then(Commands.literal("worlds")
                        .then(Commands.literal("weather")
                                .executes(ctx -> weather(ctx.getSource()))
                                .then(Commands.literal("force")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> forceWeather(ctx.getSource()))))
                        .then(Commands.literal("gateway")
                                .then(Commands.literal("travel")
                                        .then(Commands.argument("planet", ResourceLocationArgument.id())
                                                .suggests(WorldsCommands::suggestPlanetSurfaces)
                                                .executes(ctx -> gatewayTravel(ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "planet")))))
                                .then(Commands.literal("goto")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("planet", ResourceLocationArgument.id())
                                                .suggests(WorldsCommands::suggestPlanetSurfaces)
                                                .executes(ctx -> report(ctx.getSource(), GatewayDebug.goTo(
                                                        ctx.getSource().getPlayerOrException(),
                                                        ResourceLocationArgument.getId(ctx, "planet"))))))
                                .then(Commands.literal("build")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> report(ctx.getSource(), GatewayDebug.build(
                                                ctx.getSource().getPlayerOrException(), false)))
                                        .then(Commands.literal("active")
                                                .executes(ctx -> report(ctx.getSource(), GatewayDebug.build(
                                                        ctx.getSource().getPlayerOrException(), true))))))
                        .then(Commands.literal("archive")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("tablet")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests(WorldsCommands::suggestArchiveEntries)
                                                .executes(ctx -> report(ctx.getSource(), ArchiveDebug.tablet(
                                                        ctx.getSource().getPlayerOrException(),
                                                        ResourceLocationArgument.getId(ctx, "entry"))))))
                                .then(Commands.literal("plinth")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests(WorldsCommands::suggestArchiveEntries)
                                                .executes(ctx -> report(ctx.getSource(), ArchiveDebug.plinth(
                                                        ctx.getSource().getPlayerOrException(),
                                                        ResourceLocationArgument.getId(ctx, "entry")))))))));
    }

    private static CompletableFuture<Suggestions> suggestArchiveEntries(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                context.getSource().registryAccess().registryOrThrow(ArchiveEntry.REGISTRY).keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestPlanetSurfaces(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                context.getSource().registryAccess().registryOrThrow(WorldsRegistries.PLANET).stream()
                        .map(planet -> planet.surface().location()),
                builder);
    }

    /** A debug command's outcome: null is success, anything else is why it failed. */
    private static int report(CommandSourceStack source, Component failure) {
        if (failure != null) {
            source.sendFailure(failure);
            return 0;
        }
        return 1;
    }

    /**
     * Typed gateway travel. Anyone can type it, so it goes through the same
     * {@link GatewayDestinations#tryTravel} check the destination screen does &mdash; without it,
     * this would be a free teleport around the entire spaceflight loop. Operators (permission 2)
     * skip the picker-session part as a debug tool, like {@code weather force}.
     */
    private static int gatewayTravel(CommandSourceStack source, ResourceLocation planetDimension)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Component refusal = GatewayDestinations.tryTravel(player, planetDimension, source.hasPermission(2));
        if (refusal != null) {
            source.sendFailure(refusal);
            return 0;
        }
        return 1;
    }

    private static int weather(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Planet.Weather config = PlanetWeathers.get(level.dimension());
        if (config == null) {
            source.sendSuccess(() -> Component.literal(
                    level.dimension().location() + " has no weather configured."), false);
            return 1;
        }

        PlanetWeatherState state = level.getData(SpaceAttachments.WEATHER_STATE);
        source.sendSuccess(() -> Component.literal(config.type().toString() + ": ")
                .append(state.active()
                        ? Component.literal("ACTIVE").withStyle(ChatFormatting.GOLD)
                        : Component.literal("calm").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(", " + state.ticksUntilChange() + " ticks until "
                        + (state.active() ? "it ends" : "it starts"))
                        .withStyle(ChatFormatting.DARK_GRAY)),
                false);
        return 1;
    }

    /** Flips the current level's weather state on the very next tick. */
    private static int forceWeather(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        if (PlanetWeathers.get(level.dimension()) == null) {
            source.sendFailure(Component.literal(
                    level.dimension().location() + " has no weather configured."));
            return 0;
        }

        PlanetWeatherState state = level.getData(SpaceAttachments.WEATHER_STATE);
        state.forceChange();
        // The player asking is very likely the only one testing; tell them immediately rather
        // than waiting up to a tick for PlanetWeatherMechanics' own broadcast, which fires off
        // the *next* tick anyway once forceChange() takes effect there.
        PacketDistributor.sendToPlayer(player, new PlanetWeatherSyncPayload(!state.active()));
        source.sendSuccess(() -> Component.literal("Forcing weather change next tick."), true);
        return 1;
    }
}
