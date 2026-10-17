package com.ascension.combat.internal.rune.reaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The arithmetic inside Reaver's runes (docs/technical/rune-system.md section 4.2). Pure numbers (ADR-0008). */
class ReaverMathTest {

    @Test
    void ambushIsTheBaseAndMoreOnACrit() {
        assertEquals(0.40f, AmbushEffects.bonus(0.40, 0.20, false), 1e-6);
        assertEquals(0.60f, AmbushEffects.bonus(0.40, 0.20, true), 1e-6);
    }

    @Test
    void aHeadshotIsTheBaseUpToTheStartDistance() {
        assertEquals(0.5f, DeadeyeEffects.bonus(4.0, 0.5, 0.02, 10.0, 1.0), 1e-6);
        assertEquals(0.5f, DeadeyeEffects.bonus(10.0, 0.5, 0.02, 10.0, 1.0), 1e-6);
    }

    @Test
    void aHeadshotGrowsWithEveryBlockPastTheStartAndStopsAtTheMax() {
        assertEquals(0.7f, DeadeyeEffects.bonus(20.0, 0.5, 0.02, 10.0, 1.0), 1e-6);
        assertEquals(1.0f, DeadeyeEffects.bonus(60.0, 0.5, 0.02, 10.0, 1.0), 1e-6);
        assertEquals(1.0f, DeadeyeEffects.bonus(500.0, 0.5, 0.02, 10.0, 1.0), 1e-6);
    }

    @Test
    void finishingBlowIsBelowTheThresholdOnly() {
        assertTrue(FinishingBlowEffects.isNearlyDead(7.9, 20.0, 0.4));
        assertFalse(FinishingBlowEffects.isNearlyDead(8.0, 20.0, 0.4));
        assertFalse(FinishingBlowEffects.isNearlyDead(20.0, 20.0, 0.4));
    }

    @Test
    void anEnemyWithNoMaxHealthIsNeverNearlyDead() {
        assertFalse(FinishingBlowEffects.isNearlyDead(0.0, 0.0, 0.4));
    }

    @Test
    void overkillIsWhatSpilledOverUpToTheMax() {
        assertEquals(4.0f, OverkillEffects.splash(10.0f, 6.0f, 6.0f), 1e-6);
        assertEquals(6.0f, OverkillEffects.splash(50.0f, 6.0f, 6.0f), 1e-6);
    }

    @Test
    void aHitThatDoesNotKillSplashesNothing() {
        assertEquals(0.0f, OverkillEffects.splash(5.0f, 6.0f, 6.0f), 1e-6);
        assertEquals(0.0f, OverkillEffects.splash(6.0f, 6.0f, 6.0f), 1e-6);
    }
}
