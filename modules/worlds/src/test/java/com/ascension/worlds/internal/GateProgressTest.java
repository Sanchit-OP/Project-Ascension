package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The mark's stage from the number of gates lit: one glyph per gate, none before the first, and no
 * picture past the seventh (docs/lore/the-mark.md; the art has stages 1 to 7).
 */
class GateProgressTest {

    @Test
    void noGatesNoMark() {
        assertEquals(0, GateProgress.markStage(0));
    }

    @Test
    void everyGateAddsOneGlyph() {
        for (int lit = 1; lit <= 7; lit++) {
            assertEquals(lit, GateProgress.markStage(lit));
        }
    }

    @Test
    void stopsAtTheSeventh() {
        assertEquals(GateProgress.MAX_STAGE, GateProgress.markStage(8));
        assertEquals(GateProgress.MAX_STAGE, GateProgress.markStage(40));
    }

    @Test
    void neverNegative() {
        assertEquals(0, GateProgress.markStage(-1));
    }
}
