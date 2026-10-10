package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The mark's stage from the number of gates lit: one glyph per gate, none before the first, and no
 * picture past the seventh (docs/lore/the-mark.md; the art has stages 1 to 7). The pylon's carved
 * glyphs follow the same count.
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

    @Test
    void glyphKGlowsOnceGateKIsLit() {
        assertFalse(GateProgress.glyphLit(0, 0), "no gate lit: the pylon is dark");
        assertTrue(GateProgress.glyphLit(0, 1), "Earth's ring lights glyph 0");
        assertFalse(GateProgress.glyphLit(1, 1));
        assertTrue(GateProgress.glyphLit(1, 2), "the Moon's lights glyph 1");
        for (int glyph = 0; glyph < 7; glyph++) {
            assertTrue(GateProgress.glyphLit(glyph, GateProgress.MAX_STAGE), "every glyph at the full mark");
        }
    }
}
