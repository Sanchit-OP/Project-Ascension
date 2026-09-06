package com.ascension.worlds.internal;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.internal.net.PlanetWeatherSyncPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Debug and diagnostic commands, mirroring {@code AtmosphereCommands}'s reasoning: a storm timer
 * ticking silently server-side is exactly the kind of state a bug report cannot describe
 * precisely without a way to read it directly ("I think it lasted 10 seconds" is not a
 * measurement).
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
                                        .executes(ctx -> forceWeather(ctx.getSource()))))));
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
