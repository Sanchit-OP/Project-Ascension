package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.worlds.api.Planet;
import com.ascension.worlds.internal.HazardDoses.Dose;
import org.junit.jupiter.api.Test;

/**
 * When the rot doses a player. Whether a worn piece counts as gold needs real items and stays
 * for the client check; this is the timing that decides whether the rot bites at all.
 */
class HazardDosesTest {

    @Test
    void anUnwardedPlayerWithoutTheEffectStartsADose() {
        assertEquals(Dose.START, HazardDoses.next(false, false, false, 0));
    }

    @Test
    void wardedOrExemptPlayersAreNeverDosed() {
        assertEquals(Dose.NONE, HazardDoses.next(false, true, false, 0));
        assertEquals(Dose.NONE, HazardDoses.next(true, false, false, 0));
        assertEquals(Dose.NONE, HazardDoses.next(true, false, true, 5));
    }

    @Test
    void aWardedPlayerKeepsWhatIsLeftOfTheirDose() {
        // Not renewed, not cleared: putting gold on lets the rot run out.
        assertEquals(Dose.NONE, HazardDoses.next(false, true, true, 10));
    }

    @Test
    void aDoseIsLeftToCountDownUntilItWouldRunOutBeforeTheNextCheck() {
        assertEquals(Dose.NONE, HazardDoses.next(false, false, true, 100));
        assertEquals(Dose.NONE, HazardDoses.next(false, false, true, HazardDoses.CHECK_INTERVAL + 1));
        assertEquals(Dose.RENEW, HazardDoses.next(false, false, true, HazardDoses.CHECK_INTERVAL));
        assertEquals(Dose.RENEW, HazardDoses.next(false, false, true, 1));
    }

    @Test
    void anEffectThatNeverRunsOutIsLeftAlone() {
        assertEquals(Dose.NONE, HazardDoses.next(false, false, true, HazardDoses.INFINITE));
    }

    @Test
    void aDoseOutlastsTheCheckThatRenewsIt() {
        // Planet.Hazard's floor on duration_ticks: below it the effect blinks off between checks.
        assertTrue(Planet.Hazard.MIN_DURATION > HazardDoses.CHECK_INTERVAL);
    }
}
