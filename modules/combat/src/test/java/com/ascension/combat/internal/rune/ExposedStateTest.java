package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Exposed's strength and length (docs/technical/rune-system.md section 4.1). Pure numbers (ADR-0008). */
class ExposedStateTest {

    @Test
    void notExposedUntilSomethingExposesIt() {
        ExposedState state = new ExposedState();
        assertFalse(state.isActive(0));
        assertEquals(0.0f, state.amplify(0));
    }

    @Test
    void exposedUntilItsEnd() {
        ExposedState state = new ExposedState();
        state.expose(0, 100, 0.10f);
        assertTrue(state.isActive(99));
        assertEquals(0.10f, state.amplify(99));
        assertFalse(state.isActive(100));
        assertEquals(0.0f, state.amplify(100));
    }

    @Test
    void aWeakerExposureNeverCutsAStrongerOneShort() {
        ExposedState state = new ExposedState();
        state.expose(0, 100, 0.20f);
        state.expose(10, 50, 0.05f);
        assertEquals(0.20f, state.amplify(60));
        assertTrue(state.isActive(99));
    }

    @Test
    void aStrongerExposureTakesOverAndKeepsTheLaterEnd() {
        ExposedState state = new ExposedState();
        state.expose(0, 100, 0.10f);
        state.expose(10, 60, 0.30f);
        assertEquals(0.30f, state.amplify(20));
        assertTrue(state.isActive(99), "the earlier, longer exposure still runs to its end");
    }

    @Test
    void anOldExposureDoesNotKeepItsStrengthForANewOne() {
        ExposedState state = new ExposedState();
        state.expose(0, 100, 0.30f);
        state.expose(200, 300, 0.10f);
        assertEquals(0.10f, state.amplify(250));
    }
}
