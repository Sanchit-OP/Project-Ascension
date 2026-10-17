package com.ascension.combat.internal.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static final ResourceLocation ROOT = id("guard/vitality_1");
    private static final ResourceLocation PLATING = id("guard/plating_3");
    private static final ResourceLocation WARDING = id("guard/warding_3");
    private static final ResourceLocation BASTION = id("guard/bastion");
    private static final ResourceLocation HEALTH = ResourceLocation.withDefaultNamespace("generic.max_health");
    private static final ResourceLocation SPEED = ResourceLocation.withDefaultNamespace("generic.movement_speed");

    @Test
    void aNodeTouchingTheCentreNeedsOnlyAPoint() {
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(ROOT, List.of(), Set.of(), 1));
        assertEquals(GrowthRules.TakeCheck.NO_POINTS, GrowthRules.check(ROOT, List.of(), Set.of(), 0));
    }

    @Test
    void aNodeNeedsSomethingItGrowsFrom() {
        assertEquals(GrowthRules.TakeCheck.NOT_CONNECTED,
                GrowthRules.check(BASTION, List.of(PLATING, WARDING), Set.of(ROOT), 5));
    }

    @Test
    void eitherSideOfAForkIsEnough() {
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(BASTION, List.of(PLATING, WARDING), Set.of(PLATING), 1));
        assertEquals(GrowthRules.TakeCheck.OK, GrowthRules.check(BASTION, List.of(PLATING, WARDING), Set.of(WARDING), 1));
    }

    @Test
    void aTakenNodeCannotBeTakenAgain() {
        assertEquals(GrowthRules.TakeCheck.ALREADY_TAKEN, GrowthRules.check(ROOT, List.of(), Set.of(ROOT), 3));
    }

    @Test
    void beingDisconnectedIsSaidBeforeHavingNoPoints() {
        assertEquals(GrowthRules.TakeCheck.NOT_CONNECTED, GrowthRules.check(BASTION, List.of(PLATING), Set.of(), 0));
    }

    @Test
    void nodesTheDataDroppedCostNothing() {
        assertEquals(1, GrowthRules.spent(Set.of(ROOT, id("gone")), ROOT::equals));
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
