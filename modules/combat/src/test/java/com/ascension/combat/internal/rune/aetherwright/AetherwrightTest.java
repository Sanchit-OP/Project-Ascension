package com.ascension.combat.internal.rune.aetherwright;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Aetherwright's bookkeeping and arithmetic (docs/technical/rune-system.md section 4.3). Pure logic (ADR-0008). */
class AetherwrightTest {

    // --- Starfall

    @Test
    void aStarMayBeCalledUntilOneIsCalled() {
        StarfallState state = new StarfallState();
        assertTrue(state.ready(0));
        state.call(0, new Vec3(1, 2, 3), 20, 200);
        assertFalse(state.ready(1), "a star is falling");
    }

    @Test
    void aStarLandsAfterItsDelayAndOnlyThen() {
        StarfallState state = new StarfallState();
        state.call(100, new Vec3(1, 2, 3), 20, 200);
        assertFalse(state.due(119));
        assertTrue(state.due(120));
        assertEquals(new Vec3(1, 2, 3), state.land());
        assertFalse(state.falling());
        assertNull(state.where());
    }

    @Test
    void theCooldownRunsFromTheCallNotTheLanding() {
        StarfallState state = new StarfallState();
        state.call(0, Vec3.ZERO, 20, 200);
        state.land();
        assertFalse(state.ready(199));
        assertTrue(state.ready(200));
    }

    @Test
    void magicHitsShortenTheCooldown() {
        StarfallState state = new StarfallState();
        state.call(0, Vec3.ZERO, 20, 200);
        state.land();
        state.shorten(20);
        state.shorten(20);
        assertTrue(state.ready(160));
        assertFalse(state.ready(159));
    }

    @Test
    void remainingCountsDownAndIsZeroWhenNoneIsFalling() {
        StarfallState state = new StarfallState();
        assertEquals(0, state.remaining(5));
        state.call(0, Vec3.ZERO, 20, 200);
        assertEquals(15, state.remaining(5));
        assertEquals(0, state.remaining(50));
    }

    // --- Arc

    @Test
    void everyThirdHitChargesTheWeaponAndItStaysCharged() {
        ArcState state = new ArcState();
        for (int i = 0; i < 2; i++) {
            state.count(3);
            assertFalse(state.charged());
        }
        state.count(3);
        assertTrue(state.charged());
        assertTrue(state.boltReady(100, 6), "charged and never thrown: ready");
    }

    @Test
    void aBoltLeavesAtMostOnceInTheGapAndTheChargeSurvivesIt() {
        ArcState state = new ArcState();
        state.charge();
        state.bolt(100);
        assertFalse(state.boltReady(103, 6), "too soon after the last bolt");
        assertTrue(state.boltReady(106, 6));
        assertTrue(state.charged(), "a bolt does not spend the charge");
    }

    @Test
    void theEndOfTheFightDischargesAndTheCountStartsAgain() {
        ArcState state = new ArcState();
        state.count(3);
        state.count(3);
        state.count(3);
        state.discharge();
        assertFalse(state.charged());
        assertEquals(0, state.hits());
        assertFalse(state.boltReady(1000, 6), "an uncharged weapon throws nothing");
    }

    @Test
    void anItemsActiveChargesAtOnce() {
        ArcState state = new ArcState();
        state.count(3);
        state.charge();
        assertTrue(state.charged());
        assertEquals(0, state.hits());
    }

    @Test
    void fourStrikesGoRoundTheEnemiesAndEachRoundIsWeaker() {
        float[] lone = ArcEffects.shares(1, 4, 0.7f);
        assertEquals(1, lone.length);
        assertEquals(1.0f + 0.7f + 0.49f + 0.343f, lone[0], 1e-5);
        float[] pair = ArcEffects.shares(2, 4, 0.7f);
        assertEquals(1.7f, pair[0], 1e-5);
        assertEquals(1.7f, pair[1], 1e-5);
        float[] four = ArcEffects.shares(4, 4, 0.7f);
        for (float share : four) {
            assertEquals(1.0f, share, 1e-6);
        }
    }

    @Test
    void starfallsCooldownIsShortenedAtMostOnceInTheGap() {
        StarfallState state = new StarfallState();
        state.call(0, Vec3.ZERO, 20, 160);
        long start = state.cooldownUntil();
        state.shorten(20, 100, 20);
        assertEquals(start - 20, state.cooldownUntil());
        state.shorten(20, 105, 20);
        assertEquals(start - 20, state.cooldownUntil(), "a second magic hit inside the gap shortens nothing");
        state.shorten(20, 120, 20);
        assertEquals(start - 40, state.cooldownUntil());
    }

    // --- Scholar

    @Test
    void scholarAddsAShareOfExperienceRoundedToAPoint() {
        assertEquals(15, ScholarEffects.boosted(10, 0.5));
        assertEquals(2, ScholarEffects.boosted(1, 0.5), "1.5 rounds up");
    }

    @Test
    void enchantingCostsALevelLessButNeverFree() {
        assertEquals(-2, ScholarEffects.discounted(-3, 1));
        assertEquals(-1, ScholarEffects.discounted(-2, 1));
        assertEquals(-1, ScholarEffects.discounted(-1, 1), "never below a level");
        assertEquals(-1, ScholarEffects.discounted(-1, 5));
    }

    // --- Clarity

    @Test
    void clarityIsAboveTheThresholdOnly() {
        assertTrue(ClarityEffects.above(15.0, 20.0, 0.7));
        assertFalse(ClarityEffects.above(14.0, 20.0, 0.7), "exactly 70% is not above");
        assertFalse(ClarityEffects.above(10.0, 20.0, 0.7));
    }

    @Test
    void anEntityWithNoMaxHealthIsNeverClear() {
        assertFalse(ClarityEffects.above(5.0, 0.0, 0.7));
    }
}
