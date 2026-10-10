package com.ascension.compat.sable.internal;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ascension sable carry <dimension> <x y z>} (permission 2): carries the ship you stand on,
 * crew and all, to that dimension with its origin at that point. A debug command first, the way
 * {@code gateway goto} was, so the transfer can be watched on its own before ascent depends on it.
 */
public final class SableCommands {

    private SableCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SableCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascension")
                .then(Commands.literal("sable")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("carry")
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .then(Commands.argument("destination", Vec3Argument.vec3())
                                                .executes(SableCommands::carry))))));
    }

    private static int carry(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel target = DimensionArgument.getDimension(context, "dimension");
        Vec3 destination = Vec3Argument.getVec3(context, "destination");
        Component failure = ShipTransfer.carry(source.getPlayerOrException(), target, destination);
        if (failure != null) {
            source.sendFailure(failure);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Carried the ship to " + target.dimension().location()), true);
        return 1;
    }
}
