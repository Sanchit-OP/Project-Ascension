package com.ascension.combat.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.internal.rune.hud.RuneHudEntry.Kind;
import org.junit.jupiter.api.Test;

class RuneChipMathTest {

    private static final float EPS = 0.001f;

    @Test
    void stacksFillTheRingAndShowANumber() {
        assertEquals(0.4f, RuneChipMath.fraction(Kind.STACKS, 4, 10, 0, 0, 0), EPS);
        assertEquals(4, RuneChipMath.number(Kind.STACKS, 4, 0, 0));
        assertTrue(RuneChipMath.live(Kind.STACKS, 4, 0, 0));
    }

    @Test
    void noStacksIsIdleWithNoNumber() {
        assertFalse(RuneChipMath.live(Kind.STACKS, 0, 0, 0));
        assertEquals(-1, RuneChipMath.number(Kind.STACKS, 0, 0, 0));
    }

    @Test
    void aFullStackGlows() {
        assertTrue(RuneChipMath.full(Kind.STACKS, 10, 10));
        assertFalse(RuneChipMath.full(Kind.STACKS, 9, 10));
        assertFalse(RuneChipMath.full(Kind.COOLDOWN, 0, 0));
    }

    @Test
    void aChargeShowsHitsTowardTheNextPayoff() {
        assertEquals(2f / 3f, RuneChipMath.fraction(Kind.CHARGE, 2, 3, 0, 0, 0), EPS);
        assertEquals(2, RuneChipMath.number(Kind.CHARGE, 2, 0, 0));
    }

    @Test
    void aCooldownRefillsUntilReady() {
        // 100 ticks long, 25 left: three quarters refilled, not yet lit.
        assertEquals(0.75f, RuneChipMath.fraction(Kind.COOLDOWN, 0, 0, 125, 100, 100), EPS);
        assertFalse(RuneChipMath.live(Kind.COOLDOWN, 0, 125, 100));
        // Over: full ring and lit.
        assertEquals(1.0f, RuneChipMath.fraction(Kind.COOLDOWN, 0, 0, 125, 130, 100), EPS);
        assertTrue(RuneChipMath.live(Kind.COOLDOWN, 0, 125, 130));
    }

    @Test
    void aRuneNeverUsedIsReady() {
        assertTrue(RuneChipMath.live(Kind.COOLDOWN, 0, 0, 5000));
        assertEquals(1.0f, RuneChipMath.fraction(Kind.COOLDOWN, 0, 0, 0, 5000, 1), EPS);
    }

    @Test
    void aWindowDrainsUntilItCloses() {
        assertEquals(0.5f, RuneChipMath.fraction(Kind.WINDOW, 0, 0, 150, 100, 100), EPS);
        assertTrue(RuneChipMath.live(Kind.WINDOW, 0, 150, 100));
        assertEquals(0.0f, RuneChipMath.fraction(Kind.WINDOW, 0, 0, 150, 160, 100), EPS);
        assertFalse(RuneChipMath.live(Kind.WINDOW, 0, 150, 160));
    }

    @Test
    void aWindowThatCountsHitsShowsThemWhileOpen() {
        assertEquals(3, RuneChipMath.number(Kind.WINDOW, 3, 150, 100));
        assertEquals(-1, RuneChipMath.number(Kind.WINDOW, 3, 150, 200));
    }

    @Test
    void zeroLengthsDoNotDivideByZero() {
        assertEquals(0.0f, RuneChipMath.fraction(Kind.STACKS, 3, 0, 0, 0, 0), EPS);
        assertEquals(1.0f, RuneChipMath.fraction(Kind.COOLDOWN, 0, 0, 200, 100, 0), EPS);
        assertEquals(0.0f, RuneChipMath.fraction(Kind.WINDOW, 0, 0, 200, 100, 0), EPS);
    }
}
