package com.ascension.combat.internal.rune.vanguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cadence's counting (docs/technical/rune-system.md section 4.1). Pure logic; says nothing about the game (ADR-0008). */
class CadenceStateTest {

    private static final UUID ZOMBIE = UUID.randomUUID();
    private static final UUID SKELETON = UUID.randomUUID();
    private static final long WINDOW = 80; // 4 seconds

    @Test
    void theThirdHitInTheWindowCompletesTheCount() {
        CadenceState state = new CadenceState();
        assertFalse(state.hit(ZOMBIE, 0, 3, WINDOW));
        assertFalse(state.hit(ZOMBIE, 20, 3, WINDOW));
        assertTrue(state.hit(ZOMBIE, 40, 3, WINDOW));
    }

    @Test
    void completingTheCountStartsItOver() {
        CadenceState state = new CadenceState();
        state.hit(ZOMBIE, 0, 3, WINDOW);
        state.hit(ZOMBIE, 10, 3, WINDOW);
        assertTrue(state.hit(ZOMBIE, 20, 3, WINDOW));
        assertEquals(0, state.hits());
        assertFalse(state.hit(ZOMBIE, 30, 3, WINDOW));
    }

    @Test
    void hitsMustLandInsideTheWindowFromTheFirst() {
        CadenceState state = new CadenceState();
        state.hit(ZOMBIE, 0, 3, WINDOW);
        state.hit(ZOMBIE, 40, 3, WINDOW);
        assertFalse(state.hit(ZOMBIE, 80, 3, WINDOW), "the window closed at tick 80: this hit starts a new count");
        assertEquals(1, state.hits());
    }

    @Test
    void aHitOnAnotherEnemyStartsANewCount() {
        CadenceState state = new CadenceState();
        state.hit(ZOMBIE, 0, 3, WINDOW);
        state.hit(ZOMBIE, 10, 3, WINDOW);
        assertFalse(state.hit(SKELETON, 20, 3, WINDOW));
        assertEquals(1, state.hits());
        assertFalse(state.hit(ZOMBIE, 30, 3, WINDOW), "the zombie's count was lost");
    }

    @Test
    void aChargedSwingIsKnownOnlyForItsOwnTargetAndTick() {
        CadenceState state = new CadenceState();
        state.markChargedSwing(ZOMBIE, 100);
        assertTrue(state.wasChargedSwing(ZOMBIE, 100));
        assertFalse(state.wasChargedSwing(ZOMBIE, 101), "a later tick is another swing");
        assertFalse(state.wasChargedSwing(SKELETON, 100), "a sweep that hit another enemy is not the charged swing");
    }

    @Test
    void anUnmarkedSwingIsNotCharged() {
        assertFalse(new CadenceState().wasChargedSwing(ZOMBIE, 0));
    }
}
