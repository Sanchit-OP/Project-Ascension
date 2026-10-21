package com.ascension.combat.internal.rune.reaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The arithmetic inside Reaver's runes (docs/technical/rune-system.md section 4.2). Pure numbers (ADR-0008). */
class ReaverMathTest {

    @Test
    void ambushIsABaseAndMoreForEveryPointOfLethalityAndMoreOnACrit() {
        // +100% base, +2% a point: 25 Lethality is the +150% it was asked for.
        assertEquals(1.0f, AmbushEffects.bonus(1.0, 0.02, 0.2, 0.0, false), 1e-6);
        assertEquals(1.5f, AmbushEffects.bonus(1.0, 0.02, 0.2, 25.0, false), 1e-6);
        assertEquals(1.7f, AmbushEffects.bonus(1.0, 0.02, 0.2, 25.0, true), 1e-6);
        assertEquals(1.0f, AmbushEffects.bonus(1.0, 0.02, 0.2, -5.0, false), 1e-6, "negative Lethality adds nothing");
    }

    @Test
    void ambushIgnoresAShareOfArmorThatGrowsWithLethalityUpToACeiling() {
        assertEquals(0.25f, AmbushEffects.armorIgnored(0.25, 0.01, 0.75, 0.0), 1e-6);
        assertEquals(0.50f, AmbushEffects.armorIgnored(0.25, 0.01, 0.75, 25.0), 1e-6);
        assertEquals(0.75f, AmbushEffects.armorIgnored(0.25, 0.01, 0.75, 100.0), 1e-6);
    }

    @Test
    void ambushIsPrimedAfterAQuietSpellOrAMoveAndASpentHitStartsItAgain() {
        AmbushState state = new AmbushState();
        assertTrue(state.primed(1000, 120), "a fresh player is ready");
        state.hit(1000);
        assertFalse(state.primed(1050, 120), "a hit 50 ticks ago is not a quiet spell");
        assertTrue(state.primed(1120, 120), "six seconds of quiet");
        state.prime(1200);
        assertTrue(state.primed(1150, 120), "a dash opened the way");
        state.hit(1150);
        assertFalse(state.primed(1160, 120), "the hit spent the priming");
        assertEquals(10, state.readiness(1160, 120));
        assertEquals(120, state.readiness(1300, 120), "full once a hit would be primed");
    }

    @Test
    void predatorAddsAShareOfDamageForEveryPointOfLethality() {
        assertEquals(1.0f, PredatorEffects.multiplier(0.0, 0.004), 1e-6);
        assertEquals(1.24f, PredatorEffects.multiplier(60.0, 0.004), 1e-6);
        assertEquals(1.0f, PredatorEffects.multiplier(-10.0, 0.004), 1e-6);
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
