package com.ascension.combat.internal.growth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * Growth's arithmetic, with no player or level in it (docs/progression/growth.md sections 5 and 9):
 * whether a node can be taken, how many points are spent, and what a set of stats adds up to.
 * {@link PlayerGrowth} gathers the inputs from the game and applies the results; this decides.
 * Split out so the rules are checked by unit tests ({@code GrowthRulesTest}), which per ADR-0008
 * say nothing about the stats actually landing on a player.
 */
public final class GrowthRules {

    /** Every node costs one point (growth.md section 5). */
    public static final int NODE_COST = 1;

    /** What a respec costs, in XP levels, outside creative (Sanchit, 2026-10-15). */
    public static final int RESPEC_LEVELS = 20;

    private GrowthRules() {
    }

    /** Why a node can or cannot be taken. */
    public enum TakeCheck {
        OK,
        /** No such node in the data. {@link #check} never says this; {@link PlayerGrowth#take} does. */
        UNKNOWN,
        ALREADY_TAKEN,
        NOT_CONNECTED,
        NO_POINTS
    }

    /** Why a respec did or did not happen. */
    public enum RespecCheck {
        OK,
        NOTHING_TAKEN,
        IN_COMBAT,
        NOT_ENOUGH_LEVELS
    }

    /**
     * Whether a player may reset their tree: something to reset, and outside creative, out of
     * combat with {@link #RESPEC_LEVELS} levels to pay.
     */
    public static RespecCheck checkRespec(int takenCount, boolean creative, boolean inCombat, int levels) {
        if (takenCount == 0) {
            return RespecCheck.NOTHING_TAKEN;
        }
        if (creative) {
            return RespecCheck.OK;
        }
        if (inCombat) {
            return RespecCheck.IN_COMBAT;
        }
        if (levels < RESPEC_LEVELS) {
            return RespecCheck.NOT_ENOUGH_LEVELS;
        }
        return RespecCheck.OK;
    }

    /**
     * Whether {@code node} can be taken: not already taken, touching the centre (no links) or a
     * taken node it links to, and a point to pay for it.
     */
    public static TakeCheck check(ResourceLocation node, List<ResourceLocation> links, Set<ResourceLocation> taken,
            int available) {
        if (taken.contains(node)) {
            return TakeCheck.ALREADY_TAKEN;
        }
        if (!links.isEmpty() && links.stream().noneMatch(taken::contains)) {
            return TakeCheck.NOT_CONNECTED;
        }
        if (available < NODE_COST) {
            return TakeCheck.NO_POINTS;
        }
        return TakeCheck.OK;
    }

    /** Points spent on the taken nodes the data still defines; a node it dropped costs nothing. */
    public static int spent(Set<ResourceLocation> taken, Predicate<ResourceLocation> exists) {
        return (int) taken.stream().filter(exists).count() * NODE_COST;
    }

    /** Adds stats up per attribute and operation, which is how they are applied: one modifier each. */
    public static Map<GrowthStat.StatKey, Double> sum(Iterable<GrowthStat> stats) {
        Map<GrowthStat.StatKey, Double> totals = new LinkedHashMap<>();
        for (GrowthStat stat : stats) {
            totals.merge(stat.key(), stat.amount(), Double::sum);
        }
        return totals;
    }
}
