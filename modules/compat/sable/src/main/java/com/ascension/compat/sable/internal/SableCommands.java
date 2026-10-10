package com.ascension.compat.sable.internal;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ascension sable carry <dimension> <x y z>} (permission 2): carries the ship you stand on,
 * crew and all, to that dimension with its origin at that point. {@code carry ship <ship> ...}
 * names the ship with a Sable selector instead, so it also runs from a console. The same move
 * {@link ShipVoyages} makes when a ship crosses into or out of space, on demand, for testing.
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
                                                .executes(context -> carry(context, false))))
                                .then(Commands.literal("ship")
                                        .then(Commands.argument("ship", SubLevelArgumentType.singleSubLevel())
                                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                        .then(Commands.argument("destination", Vec3Argument.vec3())
                                                                .executes(context -> carry(context, true)))))))));
    }

    /**
     * {@code byName}: the ship named by a Sable selector ({@code @l}, the latest made, {@code @n},
     * the nearest...), which works from a console with no player aboard; otherwise the ship the
     * player running it stands on.
     */
    private static int carry(CommandContext<CommandSourceStack> context, boolean byName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel target = DimensionArgument.getDimension(context, "dimension");
        Vec3 destination = Vec3Argument.getVec3(context, "destination");
        ServerSubLevel ship;
        if (byName) {
            ship = SubLevelArgumentType.getSingleSubLevel(context, "ship");
        } else {
            ServerPlayer player = source.getPlayerOrException();
            ship = ShipTransfer.shipAround(player.serverLevel(), player.position());
            if (ship == null) {
                source.sendFailure(Component.literal("You are not aboard a ship."));
                return 0;
            }
        }
        Component failure = ShipTransfer.carry(ShipTransfer.board(ship), target, destination);
        if (failure != null) {
            source.sendFailure(failure);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Carried the ship to " + target.dimension().location()
                + " at " + Math.round(destination.x) + " " + Math.round(destination.y) + " " + Math.round(destination.z)), true);
        return 1;
    }
}
