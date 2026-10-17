package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import com.ascension.core.api.CombatStateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A player's growth, worked out and applied (docs/progression/growth.md section 9, ADR-0021).
 *
 * <p>Nothing here is added up and stored. Points come from the milestones whose advancements the
 * player has earned; stats come from those and the nodes in {@link GrowthState}. {@link #refresh}
 * works both out again and replaces the player's growth modifiers, and runs on every event that
 * could change them ({@link GrowthEvents}). So a balance change in the data reaches every player,
 * and a total can never drift from what earned it.
 *
 * <p>Stats land as one permanent attribute modifier per attribute and operation, with ids under
 * {@link #MODIFIER_PREFIX}. Permanent, because the player's saved attributes load before their
 * health: a transient max-HP bonus would not exist yet when a reloaded player's health is read, and
 * the health would be cut down to 20. A respawn copies only base values, so growth is applied
 * again on respawn.
 */
public final class PlayerGrowth {

    /**
     * Every growth modifier's id is {@code ascension_combat:growth/<operation>}. The stats session's
     * {@code StatSources} names this prefix "Growth" on the character sheet (item-stats.md section 6).
     */
    public static final String MODIFIER_PREFIX = "growth/";

    private PlayerGrowth() {
    }

    public static ResourceLocation modifierId(AttributeModifier.Operation operation) {
        return ResourceLocation.fromNamespaceAndPath(
                AscensionCombat.MOD_ID, MODIFIER_PREFIX + operation.getSerializedName());
    }

    public static Registry<GrowthMilestone> milestones(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(GrowthMilestone.REGISTRY);
    }

    public static Registry<GrowthNode> nodes(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(GrowthNode.REGISTRY);
    }

    public static TeamGrowthState team(MinecraftServer server) {
        return server.overworld().getData(GrowthAttachments.TEAM_GROWTH);
    }

    /**
     * What this advancement is worth: usually nothing, one milestone at most in practice. Walks the
     * registry rather than keeping an index, which would need clearing on every server stop: there
     * are a few dozen milestones, and this runs only when an advancement is earned.
     */
    public static List<GrowthMilestone> milestonesFor(MinecraftServer server, ResourceLocation advancement) {
        List<GrowthMilestone> worth = new ArrayList<>();
        for (GrowthMilestone milestone : milestones(server)) {
            if (milestone.advancement().equals(advancement)) {
                worth.add(milestone);
            }
        }
        return worth;
    }

    /** The milestones whose advancement this player has earned. */
    public static List<GrowthMilestone> earnedMilestones(ServerPlayer player) {
        List<GrowthMilestone> earned = new ArrayList<>();
        for (GrowthMilestone milestone : milestones(player.server)) {
            AdvancementHolder advancement = player.server.getAdvancements().get(milestone.advancement());
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                earned.add(milestone);
            }
        }
        return earned;
    }

    public static int pointsEarned(ServerPlayer player) {
        return earnedMilestones(player).stream().mapToInt(GrowthMilestone::points).sum();
    }

    public static int pointsSpent(ServerPlayer player) {
        Registry<GrowthNode> nodes = nodes(player.server);
        return GrowthRules.spent(player.getData(GrowthAttachments.GROWTH).taken(), nodes::containsKey);
    }

    public static int pointsAvailable(ServerPlayer player) {
        return pointsEarned(player) - pointsSpent(player);
    }

    /** The advancement's title, or its id if it has no display. */
    public static Component title(AdvancementHolder advancement) {
        return advancement.value().display()
                .map(DisplayInfo::getTitle)
                .orElseGet(() -> Component.literal(advancement.id().toString()));
    }

    /** Takes one node for the player, if the rules allow it. */
    public static GrowthRules.TakeCheck take(ServerPlayer player, ResourceLocation nodeId) {
        GrowthNode node = nodes(player.server).get(nodeId);
        if (node == null) {
            return GrowthRules.TakeCheck.UNKNOWN;
        }
        GrowthState state = player.getData(GrowthAttachments.GROWTH);
        GrowthRules.TakeCheck check = GrowthRules.check(nodeId, node.links(), state.taken(), pointsAvailable(player));
        if (check == GrowthRules.TakeCheck.OK) {
            state.take(nodeId);
            refresh(player, true);
        }
        return check;
    }

    /** Resets the player's tree, every point back to spend, if the rules allow it and they can pay. */
    public static GrowthRules.RespecCheck respec(ServerPlayer player) {
        GrowthState state = player.getData(GrowthAttachments.GROWTH);
        boolean creative = player.getAbilities().instabuild;
        GrowthRules.RespecCheck check = GrowthRules.checkRespec(
                state.taken().size(), creative, CombatStateRegistry.isInCombat(player), player.experienceLevel);
        if (check == GrowthRules.RespecCheck.OK) {
            if (!creative) {
                player.giveExperienceLevels(-GrowthRules.RESPEC_LEVELS);
            }
            reset(player);
        }
        return check;
    }

    /** Clears the player's tree with no rules and no cost: the admin's reset. */
    public static void reset(ServerPlayer player) {
        player.getData(GrowthAttachments.GROWTH).clear();
        refresh(player, false);
    }

    /**
     * A shared milestone reaches every player: awards each criterion this player is missing. Fires
     * the player's own advancement event, which refreshes their growth ({@link GrowthEvents}).
     */
    public static void awardIfMissing(ServerPlayer player, AdvancementHolder advancement) {
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        List<String> missing = new ArrayList<>();
        progress.getRemainingCriteria().forEach(missing::add);
        for (String criterion : missing) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /**
     * Works the player's growth out again and applies it.
     *
     * <p>Nodes the data no longer defines are dropped, giving their points back. If the player has
     * spent more points than they now have (an advancement revoked, or fewer points in the data),
     * their whole tree is given back to spend again, and they are told.
     *
     * @param healGains true when the player has just grown: any max HP gained is healed, so a new
     *     heart arrives full. False on login and respawn, where health is already right.
     */
    public static void refresh(ServerPlayer player, boolean healGains) {
        Registry<GrowthNode> nodes = nodes(player.server);
        GrowthState state = player.getData(GrowthAttachments.GROWTH);
        List<GrowthMilestone> earned = earnedMilestones(player);

        boolean dropped = state.retainOnly(nodes.keySet());
        int points = earned.stream().mapToInt(GrowthMilestone::points).sum();
        if (GrowthRules.spent(state.taken(), nodes::containsKey) > points) {
            state.clear();
            dropped = true;
        }
        if (dropped) {
            player.sendSystemMessage(Component.translatable("growth.ascension_combat.tree_given_back"));
        }

        apply(player, totals(earned, state, nodes), healGains);
    }

    /** What the player's growth adds up to, per attribute and operation. */
    public static Map<GrowthStat.StatKey, Double> totals(ServerPlayer player) {
        return totals(earnedMilestones(player), player.getData(GrowthAttachments.GROWTH), nodes(player.server));
    }

    private static Map<GrowthStat.StatKey, Double> totals(List<GrowthMilestone> earned, GrowthState state,
            Registry<GrowthNode> nodes) {
        List<GrowthStat> stats = new ArrayList<>();
        earned.forEach(milestone -> stats.addAll(milestone.stats()));
        for (ResourceLocation nodeId : state.taken()) {
            GrowthNode node = nodes.get(nodeId);
            if (node != null) {
                stats.addAll(node.stats());
            }
        }
        return GrowthRules.sum(stats);
    }

    /**
     * Sets each attribute's growth modifiers to the totals: added or replaced where there is a total,
     * removed where there is none. Walks every registered attribute, so a modifier left by an
     * attribute the data no longer names is removed too. A modifier already at its total is left
     * alone, so nothing is re-sent to the client for no reason.
     */
    private static void apply(ServerPlayer player, Map<GrowthStat.StatKey, Double> totals, boolean healGains) {
        float maxHealthBefore = player.getMaxHealth();
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> applyTo(player, attribute, totals));
        float gained = player.getMaxHealth() - maxHealthBefore;
        if (healGains && gained > 0) {
            player.heal(gained);
        }
    }

    private static void applyTo(ServerPlayer player, Holder.Reference<Attribute> attribute,
            Map<GrowthStat.StatKey, Double> totals) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
            ResourceLocation id = modifierId(operation);
            Double total = totals.get(new GrowthStat.StatKey(attribute.key().location(), operation));
            if (total == null || total == 0.0) {
                instance.removeModifier(id);
                continue;
            }
            AttributeModifier current = instance.getModifier(id);
            if (current == null || current.amount() != total) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(id, total, operation));
            }
        }
    }
}
