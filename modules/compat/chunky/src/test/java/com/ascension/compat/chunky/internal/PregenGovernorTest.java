package com.ascension.compat.chunky.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.compat.chunky.internal.PregenGovernor.Action;
import org.junit.jupiter.api.Test;

class PregenGovernorTest {

    @Test
    void carriesOnWhileTheServerKeepsUp() {
        assertEquals(Action.NONE, PregenGovernor.decide(true, false, 12.0));
        assertEquals(Action.NONE, PregenGovernor.decide(true, false, PregenGovernor.PAUSE_ABOVE_MS));
    }

    @Test
    void pausesWhenTicksFallBehind() {
        assertEquals(Action.PAUSE, PregenGovernor.decide(true, false, 41.0));
        assertEquals(Action.PAUSE, PregenGovernor.decide(true, false, 394.0)); // the 2026-10-10 crash run
    }

    @Test
    void resumesOnlyOnceTheServerHasRecovered() {
        assertEquals(Action.NONE, PregenGovernor.decide(false, true, 35.0), "between the two limits: wait");
        assertEquals(Action.NONE, PregenGovernor.decide(false, true, PregenGovernor.RESUME_BELOW_MS));
        assertEquals(Action.RESUME, PregenGovernor.decide(false, true, 10.0));
    }

    @Test
    void neverResumesATaskItDidNotPause() {
        assertEquals(Action.NONE, PregenGovernor.decide(false, false, 1.0), "paused by an admin, or none at all");
    }

    @Test
    void theLimitsLeaveAGapSoItCannotFlap() {
        assertEquals(true, PregenGovernor.RESUME_BELOW_MS < PregenGovernor.PAUSE_ABOVE_MS);
        assertEquals(true, PregenGovernor.PAUSE_ABOVE_MS < 50.0, "pauses before a tick overruns");
    }
}
