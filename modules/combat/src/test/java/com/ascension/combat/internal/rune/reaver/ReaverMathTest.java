package com.ascension.combat.internal.rune.reaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The arithmetic inside Reaver's runes (docs/technical/rune-system.md section 4.2). Pure numbers (ADR-0008). */
class ReaverMathTest {

    @Test
    void ambushIsTheBaseAndMoreOnACrit() {
        assertEquals(0.40f, AmbushEffects.bonus(0.40, 0.20, false), 1e-6);
        assertEquals(0.60f, AmbushEffects.bonus(0.40, 0.20, true), 1e-6);
    }

    @Test
    void distanceAddsNothingUpToTheStartDistance() {
        assertEquals(0.0f, DeadeyeEffects.distanceBonus(4.0, 0.02, 10.0, 0.5), 1e-6);
        assertEquals(0.0f, DeadeyeEffects.distanceBonus(10.0, 0.02, 10.0, 0.5), 1e-6);
    }

    @Test
    void distanceGrowsWithEveryBlockPastTheStartAndStopsAtTheMax() {
        assertEquals(0.2f, DeadeyeEffects.distanceBonus(20.0, 0.02, 10.0, 0.5), 1e-6);
        assertEquals(0.5f, DeadeyeEffects.distanceBonus(60.0, 0.02, 10.0, 0.5), 1e-6);
        assertEquals(0.5f, DeadeyeEffects.distanceBonus(500.0, 0.02, 10.0, 0.5), 1e-6);
    }

    @Test
    void aHeadshotNoteIsReadOnceAndOnlyForTheEnemyAndTickItWasMadeFor() {
        UUID zombie = UUID.randomUUID();
        UUID skeleton = UUID.randomUUID();
        HeadshotMark mark = new HeadshotMark();
        assertFalse(mark.consume(zombie, 100L), "nothing was marked yet");

        mark.note(zombie, 100L);
        assertFalse(mark.consume(skeleton, 100L), "a different enemy");
        mark.note(zombie, 100L);
        assertFalse(mark.consume(zombie, 101L), "a later tick");
        mark.note(zombie, 100L);
        assertTrue(mark.consume(zombie, 100L));
        assertFalse(mark.consume(zombie, 100L), "reading it used it up");
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
