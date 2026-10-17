package com.ascension.combat.internal.rune.vanguard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Giant Slayer's bonus curve (docs/technical/rune-system.md section 4.1). Pure logic; says nothing about the game (ADR-0008). */
class GiantSlayerEffectsTest {

    private static double bonus(double ratio) {
        return GiantSlayerEffects.bonus(ratio, 1.1, 0.05, 2.0, 0.15);
    }

    @Test
    void nothingAgainstEnemiesNotBiggerEnough() {
        assertEquals(0.0, bonus(0.5));
        assertEquals(0.0, bonus(1.0));
        assertEquals(0.0, bonus(1.09));
    }

    @Test
    void theMinimumAtTheFirstThreshold() {
        assertEquals(0.05, bonus(1.1), 1e-9);
    }

    @Test
    void risesInAStraightLineBetween() {
        assertEquals(0.10, bonus(1.55), 1e-9);
    }

    @Test
    void theMaximumAtDoubleAndNeverMore() {
        assertEquals(0.15, bonus(2.0), 1e-9);
        assertEquals(0.15, bonus(40.0), 1e-9);
    }

    @Test
    void aBackwardsCurveInADatapackStillGivesTheMaximum() {
        assertEquals(0.15, GiantSlayerEffects.bonus(2.5, 2.0, 0.05, 1.1, 0.15), 1e-9);
    }
}
