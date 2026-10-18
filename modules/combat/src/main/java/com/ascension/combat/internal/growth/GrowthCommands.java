package com.ascension.combat.internal.growth;

import com.ascension.combat.internal.stat.StatText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * {@code /ascension growth}: what a player's growth is, and, until the tree screen exists
 * (growth.md section 12, step G4), how they take nodes and respec. {@code take} and {@code respec}
 * are the player's own actions under the same rules the screen will use, so they need no
 * permission; reading another player and the team commands are for operators.
 *
 * <pre>
 * /ascension growth                         your points, milestones, nodes and stats
 * /ascension growth show &lt;player&gt;           the same for someone else (op)
 * /ascension growth take &lt;node&gt;             take a node
 * /ascension growth respec                  reset your tree (out of combat, 20 levels)
 * /ascension growth reset &lt;player&gt;          reset someone's tree, free (op)
 * /ascension growth team                    the team's shared milestones (op)
 * /ascension growth team forget &lt;advancement&gt;  so it can be earned again (op)
 * </pre>
 */
final class GrowthCommands {

    private GrowthCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ascension")
                .then(Commands.literal("growth")
                        .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("show")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                        .suggests(GrowthCommands::suggestNodes)
                                        .executes(ctx -> take(ctx.getSource(), ResourceLocationArgument.getId(ctx, "node")))))
                        .then(Commands.literal("respec")
                                .executes(ctx -> respec(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("team")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> team(ctx.getSource()))
                                .then(Commands.literal("forget")
                                        .then(Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(GrowthCommands::suggestTeamMilestones)
                                                .executes(ctx -> forget(ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "advancement"))))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        int earned = PlayerGrowth.pointsEarned(player);
        int spent = PlayerGrowth.pointsSpent(player);
        List<GrowthMilestone> milestones = PlayerGrowth.earnedMilestones(player);
        var nodes = PlayerGrowth.nodes(player.server);

        MutableComponent text = Component.literal("Growth: ").withStyle(ChatFormatting.GOLD)
                .append(player.getDisplayName())
                .append(Component.literal("\nPoints: " + earned + " earned, " + spent + " spent, "
                        + (earned - spent) + " to spend").withStyle(ChatFormatting.WHITE));

        List<Component> milestoneLines = milestones.stream()
                .map(milestone -> (Component) PlayerGrowth.title(player.server.getAdvancements().get(milestone.advancement()))
                        .copy().append(Component.literal(" (+" + milestone.points() + ")")))
                .toList();
        text.append(section("Milestones", milestoneLines, "none yet"));

        List<Component> nodeLines = player.getData(GrowthAttachments.GROWTH).takenInOrder().stream()
                .map(id -> PlayerGrowth.nodeName(player.server, id))
                .toList();
        text.append(section("Tree", nodeLines, "nothing taken"));

        List<Component> statLines = PlayerGrowth.totals(player).entrySet().stream()
                .map(total -> statLine(total.getKey(), total.getValue()))
                .toList();
        text.append(section("Stats", statLines, "none"));

        source.sendSuccess(() -> text, false);
        return earned - spent;
    }

    /** A new line reading {@code Label: a, b, c}, or the {@code none} text in grey when there is nothing to list. */
    private static Component section(String label, List<Component> items, String none) {
        MutableComponent line = Component.literal("\n" + label + ": ").withStyle(ChatFormatting.GRAY);
        if (items.isEmpty()) {
            return line.append(Component.literal(none).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (int i = 0; i < items.size(); i++) {
            line.append(i == 0 ? Component.empty() : Component.literal(", ")).append(items.get(i));
        }
        return line;
    }

    private static Component statLine(GrowthStat.StatKey key, double amount) {
        boolean percent = key.operation() != AttributeModifier.Operation.ADD_VALUE;
        String number = percent ? StatText.number(amount * 100.0, 1) + "%" : StatText.number(amount, 2);
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(key.attribute());
        Component name = attribute == null
                ? Component.literal(key.attribute().toString())
                : Component.translatable(attribute.getDescriptionId());
        return Component.literal((amount >= 0 ? "+" : "") + number + " ").append(name);
    }

    private static int take(CommandSourceStack source, ResourceLocation nodeId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GrowthRules.TakeCheck check = PlayerGrowth.take(player, nodeId);
        Component name = PlayerGrowth.nodeName(player.server, nodeId);
        if (check == GrowthRules.TakeCheck.OK) {
            source.sendSuccess(() -> Component.translatable(
                    "growth.ascension_combat.take.done", name, PlayerGrowth.pointsAvailable(player)), false);
            return 1;
        }
        source.sendFailure(PlayerGrowth.takeFailure(check, name));
        return 0;
    }

    private static int respec(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GrowthRules.RespecCheck check = PlayerGrowth.respec(player);
        if (check == GrowthRules.RespecCheck.OK) {
            source.sendSuccess(() -> Component.translatable(
                    "growth.ascension_combat.respec.done", PlayerGrowth.pointsAvailable(player)), false);
            return 1;
        }
        source.sendFailure(PlayerGrowth.respecFailure(check));
        return 0;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        PlayerGrowth.reset(player);
        source.sendSuccess(() -> Component.literal("Reset ").append(player.getDisplayName())
                .append("'s skill tree: " + PlayerGrowth.pointsAvailable(player) + " points to spend"), true);
        return 1;
    }

    private static int team(CommandSourceStack source) {
        var earned = PlayerGrowth.team(source.getServer()).earned();
        source.sendSuccess(() -> Component.literal("Team milestones: "
                + (earned.isEmpty() ? "none yet" : earned.toString())), false);
        return earned.size();
    }

    private static int forget(CommandSourceStack source, ResourceLocation advancement) {
        boolean had = PlayerGrowth.team(source.getServer()).remove(advancement);
        if (!had) {
            source.sendFailure(Component.literal("The team has no milestone " + advancement));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("The team forgot " + advancement
                + ". Revoke it from players with /advancement to earn it again."), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestNodes(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(PlayerGrowth.nodes(ctx.getSource().getServer()).keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestTeamMilestones(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(PlayerGrowth.team(ctx.getSource().getServer()).earned(), builder);
    }
}
