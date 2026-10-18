package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.atmosphere.internal.OxygenEmblem.Mode;
import org.junit.jupiter.api.Test;

class OxygenEmblemTest {

    @Test
    void nothingToShowMeansHidden() {
        assertEquals(Mode.HIDDEN, OxygenEmblem.modeFor(false, true, false, false, 80, 80, 20));
    }

    @Test
    void goodAirWithTimeToSpareIsQuiet() {
        // In vacuum with a full tank: plenty of time, so no alarm.
        assertEquals(Mode.QUIET, OxygenEmblem.modeFor(true, false, false, false, 600, 800, 150));
    }

    @Test
    void refillingInBreathableAirIsQuiet() {
        assertEquals(Mode.QUIET, OxygenEmblem.modeFor(true, true, false, false, 30, 80, 8));
    }

    @Test
    void runningDownBecomesTheBigEmblem() {
        // 25 seconds left is "low".
        assertEquals(Mode.WARNING, OxygenEmblem.modeFor(true, false, false, false, 100, 800, 25));
    }

    @Test
    void halfEmptyIsAWarningEvenWithTimeLeft() {
        assertEquals(Mode.WARNING, OxygenEmblem.modeFor(true, false, false, false, 300, 800, 75));
    }

    @Test
    void tenSecondsLeftIsCritical() {
        assertEquals(Mode.CRITICAL, OxygenEmblem.modeFor(true, false, false, false, 40, 800, 10));
    }

    @Test
    void suffocatingIsAlwaysCritical() {
        assertEquals(Mode.CRITICAL, OxygenEmblem.modeFor(true, false, true, false, 0, 80, 0));
        assertEquals(Mode.CRITICAL, OxygenEmblem.modeFor(true, true, true, false, 0, 80, 0));
    }

    @Test
    void aPressurisingTankWarnsEvenWithAirLeft() {
        assertEquals(Mode.WARNING, OxygenEmblem.modeFor(true, false, false, true, 700, 800, 175));
    }

    @Test
    void shareIsClamped() {
        assertEquals(0.0f, OxygenEmblem.share(10, 0));
        assertEquals(1.0f, OxygenEmblem.share(200, 100));
        assertEquals(0.25f, OxygenEmblem.share(25, 100));
    }
}
