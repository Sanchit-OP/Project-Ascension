package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Debug and diagnostic commands.
 *
 * <p>{@code why} is not a throwaway. A third-party provider that mysteriously loses to ours is
 * undiagnosable without it, and we would be the ones fielding that bug report.
 */
public final class AtmosphereCommands {

    private AtmosphereCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascension")
                .then(Commands.literal("atmosphere")
                        .then(Commands.literal("query")
                                .executes(ctx -> query(ctx.getSource())))
                        .then(Commands.literal("why")
                                .executes(ctx -> why(ctx.getSource())))
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("vacuum")
                                        .executes(ctx -> setVacuum(ctx.getSource(), true)))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> setVacuum(ctx.getSource(), false)))
                                .then(Commands.literal("oxygen")
                                        .then(Commands.argument("units", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setOxygen(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "units"))))))));
    }

    private static int query(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Atmosphere atmosphere = AtmosphereRegistry.query(player.serverLevel(), player.position());
        OxygenState state = player.getData(AtmosphereAttachments.OXYGEN);

        source.sendSuccess(() -> Component.literal("Atmosphere: ")
                .append(atmosphere.breathable()
                        ? Component.literal("breathable").withStyle(ChatFormatting.GREEN)
                        : Component.literal("NOT breathable").withStyle(ChatFormatting.RED))
                .append(Component.literal(String.format("  drain x%.2f", atmosphere.drainMultiplier())))
                .append(Component.literal("  lungs " + state.lungUnits() + "/"
                        + AtmosphereTuning.LUNG_CAPACITY + " units")), false);
        return 1;
    }

    private static int why(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 position = player.position();
        AtmosphereContext context = new AtmosphereContext(level, position);

        var entries = ProviderRegistry.get().providerEntries();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No atmosphere providers registered; "
                    + "defaulting to breathable."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Providers, in resolution order:")
                .withStyle(ChatFormatting.GRAY), false);

        boolean decided = false;
        for (var entry : entries) {
            Optional<Atmosphere> claim = entry.provider().query(context);
            boolean winner = claim.isPresent() && !decided;
            if (winner) {
                decided = true;
            }
            final boolean isWinner = winner;
            final String detail = claim
                    .map(a -> (a.breathable() ? "breathable" : "not breathable")
                            + String.format(" x%.2f", a.drainMultiplier()))
                    .orElse("no claim");
            source.sendSuccess(() -> Component.literal(
                    String.format("  [%d] %s - %s%s",
                            entry.provider().priority(), entry.id(), detail,
                            isWinner ? "   <- WINS" : ""))
                    .withStyle(isWinner ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }

    private static int setOxygen(CommandSourceStack source, int units)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        OxygenState state = player.getData(AtmosphereAttachments.OXYGEN);
        state.setLungUnits(units);
        OxygenTracker.invalidate(player);
        source.sendSuccess(() -> Component.literal(
                "Lung reserve set to " + units + " / " + AtmosphereTuning.LUNG_CAPACITY + " units"), false);
        return 1;
    }

    private static int setVacuum(CommandSourceStack source, boolean on)
            throws CommandSyntaxException {
        ServerLevel level = source.getPlayerOrException().serverLevel();
        if (on) {
            DebugAtmosphere.setVacuum(level.dimension());
            source.sendSuccess(() -> Component.literal("Debug vacuum ON for " + level.dimension().location())
                    .withStyle(ChatFormatting.RED), true);
        } else {
            DebugAtmosphere.clear();
            source.sendSuccess(() -> Component.literal("Debug vacuum cleared")
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return 1;
    }
}
