package com.ascension.combat.internal.rune;

import com.ascension.combat.internal.CombatAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /ascension runes show [players]} (permission 2): prints a player's saved rune page and
 * its growth. Works from a console or RCON, so a page's saving, its survival across a restart and
 * across death can be checked on a dedicated server with no client (ADR-0008).
 */
public final class RuneCommands {

    private RuneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascension")
                .then(Commands.literal("runes")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("show")
                                .executes(ctx -> show(ctx.getSource(), java.util.List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))));
    }

    private static int show(CommandSourceStack source, Collection<ServerPlayer> players) throws CommandSyntaxException {
        for (ServerPlayer player : players) {
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            RunePage page = state.page();
            String line = player.getScoreboardName() + ": primary "
                    + page.primary().map(p -> p.getSerializedName()).orElse("-")
                    + " keystone " + page.keystone().map(ResourceLocation::getPath).orElse("-")
                    + " minors " + ids(page.primaryMinors())
                    + " | secondary " + page.secondary().map(p -> p.getSerializedName()).orElse("-")
                    + " minors " + ids(page.secondaryMinors())
                    + " | shards " + ids(page.shards())
                    + " | growth " + state.growthCounts();
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return players.size();
    }

    private static String ids(Collection<ResourceLocation> ids) {
        return ids.isEmpty() ? "-" : ids.stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(","));
    }
}
