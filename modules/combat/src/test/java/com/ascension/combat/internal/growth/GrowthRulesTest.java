package com.ascension.combat.internal.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

/**
 * The tree's rules and the stat sums (docs/progression/growth.md sections 5 and 9). Pure logic: says
 * nothing about stats landing on a player, which only a running server shows (ADR-0008).
 */
class GrowthRulesTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ascension_combat", path);
    }

    private static final ResourceLocation A = id("web/a");
    private static final ResourceLocation B = id("web/b");
    private static final ResourceLocation C = id("web/c");
    private static final ResourceLocation D = id("web/d");
    private static final ResourceLocation HEALTH = ResourceLocation.withDefaultNamespace("generic.max_health");
    private static final ResourceLocation SPEED = ResourceLocation.withDefaultNamespace("generic.movement_speed");

    /** A start node a, with b and c beside it, and d beside both of those: a diamond. */
    private static Map<ResourceLocation, List<ResourceLocation>> diamond() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        links.put(A, List.of());
        links.put(B, List.of(A));
        links.put(C, List.of(A));
        links.put(D, List.of(B, C));
        return links;
    }

    @Test
    void aStartNodeNeedsOnlyAPoint() {
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(A, true, Set.of(), Set.of(), 1));
        assertEquals(GrowthRules.TakeCheck.NO_POINTS, GrowthRules.check(A, true, Set.of(), Set.of(), 0));
    }

    @Test
    void aNodeNeedsANeighbourTaken() {
        assertEquals(GrowthRules.TakeCheck.NOT_CONNECTED, GrowthRules.check(D, false, Set.of(B, C), Set.of(A), 5));
    }

    @Test
    void anyOneNeighbourIsEnough() {
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(D, false, Set.of(B, C), Set.of(B), 1));
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(D, false, Set.of(B, C), Set.of(C), 1));
    }

    @Test
    void aTakenNodeCannotBeTakenAgain() {
        assertEquals(GrowthRules.TakeCheck.ALREADY_TAKEN, GrowthRules.check(A, true, Set.of(), Set.of(A), 3));
    }

    @Test
    void beingDisconnectedIsSaidBeforeHavingNoPoints() {
        assertEquals(GrowthRules.TakeCheck.NOT_CONNECTED, GrowthRules.check(D, false, Set.of(B), Set.of(), 0));
    }

    @Test
    void aLinkJoinsBothEndsWhicheverNamesIt() {
        Map<ResourceLocation, Set<ResourceLocation>> near = GrowthRules.neighbours(diamond());
        assertEquals(Set.of(B, C), near.get(A), "a never names b or c, but they name a");
        assertEquals(Set.of(A, D), near.get(B));
        assertEquals(Set.of(B, C), near.get(D));
    }

    @Test
    void aLinkToANodeThatDoesNotExistIsDropped() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        links.put(A, List.of(id("web/missing")));
        assertEquals(Set.of(), GrowthRules.neighbours(links).get(A));
    }

    @Test
    void aPlayerCanWalkSidewaysThroughANodeTheOtherSideNamed() {
        // b names a, not the other way round, and a is taken: b is next to a all the same.
        Map<ResourceLocation, Set<ResourceLocation>> near = GrowthRules.neighbours(diamond());
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(B, false, near.get(B), Set.of(A), 1));
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(A, true, near.get(A), Set.of(B), 1));
    }

    @Test
    void everythingJoinedToAStartIsReachable() {
        assertEquals(Set.of(), GrowthRules.unreachable(Set.of(A), diamond()));
    }

    @Test
    void aNodeReachableOnlyBecauseAnotherNamesItStillCounts() {
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        links.put(A, List.of(B));   // the start names its neighbour
        links.put(B, List.of());
        assertEquals(Set.of(), GrowthRules.unreachable(Set.of(A), links));
    }

    @Test
    void nodesJoinedOnlyToEachOtherCanNeverBeTaken() {
        Map<ResourceLocation, List<ResourceLocation>> links = diamond();
        links.put(id("web/x"), List.of(id("web/y")));
        links.put(id("web/y"), List.of(id("web/x")));
        assertEquals(Set.of(id("web/x"), id("web/y")), GrowthRules.unreachable(Set.of(A), links));
    }

    @Test
    void aLinkToANodeThatDoesNotExistLeadsNowhere() {
        Map<ResourceLocation, List<ResourceLocation>> links = diamond();
        links.put(id("web/typo"), List.of(id("web/missing")));
        links.put(id("web/after_typo"), List.of(id("web/typo")));
        assertEquals(Set.of(id("web/typo"), id("web/after_typo")), GrowthRules.unreachable(Set.of(A), links));
    }

    @Test
    void withNoStartNodeNothingCanBeTaken() {
        assertEquals(Set.of(A, B, C, D), GrowthRules.unreachable(Set.of(), diamond()));
    }

    @Test
    void nodesTheDataDroppedCostNothing() {
        assertEquals(1, GrowthRules.spent(Set.of(A, id("gone")), A::equals));
    }

    @Test
    void statsAddUpPerAttributeAndOperation() {
        Map<GrowthStat.StatKey, Double> totals = GrowthRules.sum(List.of(
                new GrowthStat(HEALTH, 1, AttributeModifier.Operation.ADD_VALUE),
                new GrowthStat(HEALTH, 4, AttributeModifier.Operation.ADD_VALUE),
                new GrowthStat(SPEED, 0.02, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new GrowthStat(SPEED, 0.01, AttributeModifier.Operation.ADD_VALUE)));
        assertEquals(3, totals.size());
        assertEquals(5.0, totals.get(new GrowthStat.StatKey(HEALTH, AttributeModifier.Operation.ADD_VALUE)));
        assertEquals(0.02, totals.get(new GrowthStat.StatKey(SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)));
    }

    @Test
    void aRespecNeedsSomethingToResetThenPeaceThenLevels() {
        assertEquals(GrowthRules.RespecCheck.NOTHING_TAKEN, GrowthRules.checkRespec(0, false, false, 30));
        assertEquals(GrowthRules.RespecCheck.IN_COMBAT, GrowthRules.checkRespec(3, false, true, 30));
        assertEquals(GrowthRules.RespecCheck.NOT_ENOUGH_LEVELS, GrowthRules.checkRespec(3, false, false, 19));
        assertEquals(GrowthRules.RespecCheck.OK, GrowthRules.checkRespec(3, false, false, 20));
    }

    @Test
    void creativeRespecsFreeEvenInCombat() {
        assertEquals(GrowthRules.RespecCheck.OK, GrowthRules.checkRespec(3, true, true, 0));
    }
}
