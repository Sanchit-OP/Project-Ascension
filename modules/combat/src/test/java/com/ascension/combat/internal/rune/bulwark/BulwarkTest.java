package com.ascension.combat.internal.rune.bulwark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bulwark's bookkeeping and arithmetic (docs/technical/rune-system.md section 4.4). Pure logic (ADR-0008). */
class BulwarkTest {

    // --- Brace

    @Test
    void aBraceMayBeginUntilOneIsUnderWay() {
        BraceState state = new BraceState();
        assertTrue(state.ready(0));
        state.start(0, 50, 240);
        assertFalse(state.ready(1));
        assertTrue(state.bracing());
    }

    @Test
    void aBraceIsOverAfterItsLengthAndTheCooldownRunsFromItsStart() {
        BraceState state = new BraceState();
        state.start(100, 50, 240);
        assertFalse(state.over(149));
        assertTrue(state.over(150));
        state.end();
        assertFalse(state.bracing());
        assertFalse(state.ready(339));
        assertTrue(state.ready(340));
    }

    @Test
    void aBigHitIsAShareOfMaxHealth() {
        assertTrue(BraceEffects.isBig(5.0, 20.0, 0.25));
        assertFalse(BraceEffects.isBig(4.9, 20.0, 0.25));
        assertFalse(BraceEffects.isBig(100.0, 0.0, 0.25));
    }

    // --- Plating

    @Test
    void platingSoftensTheNextHitsInItsWindowOnly() {
        PlatingState state = new PlatingState();
        state.start(0, 3, 60, 240);
        assertTrue(state.active(30));
        state.consume();
        state.consume();
        assertTrue(state.active(30));
        state.consume();
        assertFalse(state.active(30), "three hits spent");
    }

    @Test
    void platingEndsWhenItsWindowDoes() {
        PlatingState state = new PlatingState();
        state.start(0, 3, 60, 240);
        assertTrue(state.active(59));
        assertFalse(state.active(60));
    }

    @Test
    void platingRestsBeforeItCanStartAgain() {
        PlatingState state = new PlatingState();
        state.start(0, 3, 60, 240);
        assertFalse(state.ready(100), "still resting");
        assertTrue(state.ready(240));
    }

    @Test
    void platingCannotStartWhileItIsActive() {
        PlatingState state = new PlatingState();
        state.start(0, 3, 60, 0);
        assertFalse(state.ready(10), "a window is open");
    }

    // --- Challenge

    @Test
    void aChallengeHoldsForItsTimeThenLapses() {
        ChallengeState state = new ChallengeState();
        UUID player = UUID.randomUUID();
        assertFalse(state.active(0));
        state.challenge(player, 80);
        assertTrue(state.active(79));
        assertEquals(player, state.challenger());
        assertFalse(state.active(80));
    }

    @Test
    void aMobNeverChallengedHasNoChallenger() {
        assertNull(new ChallengeState().challenger());
    }

    // --- Unbowed, Riposte, Revitalize

    @Test
    void unbowedCutsADurationByItsShareButNotBelowATick() {
        assertEquals(70, UnbowedEffects.shortened(100, 0.30));
        assertEquals(1, UnbowedEffects.shortened(1, 0.99));
    }

    @Test
    void aParryIsAShieldRaisedJustInTime() {
        assertTrue(RiposteEffects.isParry(4, 10));
        assertTrue(RiposteEffects.isParry(10, 10));
        assertFalse(RiposteEffects.isParry(11, 10));
    }

    @Test
    void revitalizeIsStrongerWhenLow() {
        assertEquals(1.10f, RevitalizeEffects.multiplier(18.0, 20.0, 0.10, 0.20, 0.40), 1e-6);
        assertEquals(1.20f, RevitalizeEffects.multiplier(7.0, 20.0, 0.10, 0.20, 0.40), 1e-6);
        assertEquals(1.10f, RevitalizeEffects.multiplier(8.0, 20.0, 0.10, 0.20, 0.40), 1e-6, "exactly 40% is not below");
    }
}
