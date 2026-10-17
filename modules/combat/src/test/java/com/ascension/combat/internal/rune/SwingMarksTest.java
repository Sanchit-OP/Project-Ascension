package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** What a swing was, for the damage event that follows it. Pure bookkeeping (ADR-0008). */
class SwingMarksTest {

    private static final UUID ZOMBIE = UUID.randomUUID();
    private static final UUID SKELETON = UUID.randomUUID();

    @Test
    void aChargedSwingIsKnownForItsTargetAndTickOnly() {
        SwingMarks marks = new SwingMarks();
        marks.swing(ZOMBIE, 100, true);
        assertTrue(marks.charged(ZOMBIE, 100));
        assertFalse(marks.charged(ZOMBIE, 101), "a later tick is another swing");
        assertFalse(marks.charged(SKELETON, 100), "a sweep that hit another enemy is not the swing that was marked");
    }

    @Test
    void anUnchargedSwingIsNotCharged() {
        SwingMarks marks = new SwingMarks();
        marks.swing(ZOMBIE, 100, false);
        assertFalse(marks.charged(ZOMBIE, 100));
    }

    @Test
    void aCritIsMarkedOnTheSwingItBelongsTo() {
        SwingMarks marks = new SwingMarks();
        marks.swing(ZOMBIE, 100, true);
        assertFalse(marks.crit(ZOMBIE, 100));
        marks.markCrit(ZOMBIE, 100);
        assertTrue(marks.crit(ZOMBIE, 100));
        assertTrue(marks.charged(ZOMBIE, 100), "marking the crit keeps the swing's charge");
    }

    @Test
    void aNewSwingForgetsTheLastOnesCrit() {
        SwingMarks marks = new SwingMarks();
        marks.swing(ZOMBIE, 100, false);
        marks.markCrit(ZOMBIE, 100);
        marks.swing(ZOMBIE, 120, false);
        assertFalse(marks.crit(ZOMBIE, 120));
    }

    @Test
    void aCritWithNoSwingNotedStillCountsForItsOwnTick() {
        SwingMarks marks = new SwingMarks();
        marks.markCrit(ZOMBIE, 50);
        assertTrue(marks.crit(ZOMBIE, 50));
        assertFalse(marks.charged(ZOMBIE, 50));
    }
}
