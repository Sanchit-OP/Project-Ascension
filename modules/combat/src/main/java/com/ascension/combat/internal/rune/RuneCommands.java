package com.ascension.combat.internal.rune;

import com.ascension.combat.internal.CombatAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /ascension runes show [players]} (permission 2): prints a player's saved rune page and
 * its growth. {@code /ascension runes list}: prints every rune the server loaded, so a bad data
 * file shows up as a missing line. Both work from a console or RCON, so a page's saving, its
 * survival across a restart and across death can be checked on a dedicated server with no client
 * (ADR-0008).
 */
public final class RuneCommands {

    private RuneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascension")
                .then(Commands.literal("runes")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("show")
                                .executes(ctx -> show(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))));
    }

    private static int show(CommandSourceStack source, Collection<ServerPlayer> players) throws CommandSyntaxException {
        for (ServerPlayer player : players) {
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            RunePage page = state.page();
            RunePageRules.OpenSlots open = RuneUnlocks.open(player);
            String line = player.getScoreboardName() + " (open: secondary " + open.secondary()
                    + ", shards " + open.offence() + "/" + open.flex() + "/" + open.defence() + "): primary "
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

    private static int list(CommandSourceStack source) {
        var runes = RuneRegistries.runes(source.registryAccess());
        runes.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(e -> {
                    RuneDefinition rune = e.getValue();
                    String line = e.getKey().location() + ": "
                            + rune.path().map(p -> p.getSerializedName()).orElse("shard")
                            + " " + rune.slot().getSerializedName() + " row " + rune.row()
                            + (rune.world() ? " (world)" : "") + (rune.grows() ? " (grows)" : "");
                    source.sendSuccess(() -> Component.literal(line), false);
                });
        return runes.size();
    }

    private static String ids(Collection<ResourceLocation> ids) {
        return ids.isEmpty() ? "-" : ids.stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(","));
    }
}
