package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tuning table, and the relationships between its numbers.
 *
 * <p>Deliberately not a restatement of the constants &mdash; a test asserting
 * {@code BASE_UNITS_PER_SECOND == 4} fails on every rebalance and catches nothing, because the
 * fix is always to update the test. What is worth pinning is the <em>design intent</em> the
 * numbers encode, most of which lives in ADRs and gameplay docs rather than in the code: a tank
 * is five minutes, lungs are twenty seconds, and the pressurise delay has to fit inside the lung
 * reserve or ADR-0009's whole argument collapses.
 *
 * <p>Those survive rebalancing and fail loudly when someone moves one number without the other.
 */
class AtmosphereTuningTest {

    @Test
    @DisplayName("a full tank is five minutes of air at baseline drain")
    void tankIsFiveMinutes() {
        assertEquals(300, AtmosphereTuning.secondsRemaining(
                AtmosphereTuning.TANK_CAPACITY, AtmosphereTuning.BASE_UNITS_PER_SECOND));
    }

    @Test
    @DisplayName("lungs are twenty seconds of air at baseline drain")
    void lungsAreTwentySeconds() {
        assertEquals(20, AtmosphereTuning.secondsRemaining(
                AtmosphereTuning.LUNG_CAPACITY, AtmosphereTuning.BASE_UNITS_PER_SECOND));
    }

    /**
     * ADR-0009's central claim: opening a fresh valve costs real time, but less time than the
     * reserve you are holding while you do it. If the delay ever grew past the lung supply, a
     * mid-crisis tank swap would stop being a decision and become an unavoidable death.
     */
    @Test
    @DisplayName("the pressurise delay fits inside the lung reserve, with room to spare")
    void pressurisingIsSurvivableOnLungsAlone() {
        int lungTicks = AtmosphereTuning.secondsRemaining(
                AtmosphereTuning.LUNG_CAPACITY, AtmosphereTuning.BASE_UNITS_PER_SECOND) * 20;
        assertTrue(AtmosphereTuning.TANK_PRESSURISE_TICKS < lungTicks,
                "pressurise delay " + AtmosphereTuning.TANK_PRESSURISE_TICKS
                        + " ticks must be shorter than the " + lungTicks
                        + " ticks of lung reserve it is meant to be survived on");
        assertTrue(AtmosphereTuning.TANK_PRESSURISE_TICKS * 4 < lungTicks,
                "the delay should be a fraction of the reserve, not most of it");
    }

    /**
     * The failure window is measured in accounting passes, because that is the only clock that
     * ever increments it. A grace that is not a whole number of passes cannot be hit exactly,
     * and the player would take their first damage early or late depending on arithmetic nobody
     * intended to write.
     */
    @Test
    @DisplayName("timings are whole numbers of accounting passes")
    void timingsAlignToTheAccountingClock() {
        assertEquals(0,
                AtmosphereTuning.SUFFOCATION_GRACE_TICKS % AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS,
                "grace window must be a whole number of accounting passes");
        assertEquals(0,
                AtmosphereTuning.TANK_PRESSURISE_TICKS % AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS,
                "pressurise delay is decremented one pass at a time and would otherwise "
                        + "overshoot past zero");
    }

    @Test
    @DisplayName("accounting runs at 2 Hz, not every tick")
    void accountingIsSubTickRate() {
        assertEquals(0.5f, AtmosphereTuning.accountingSeconds(), 0.0001f);
        assertTrue(AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS > 1,
                "ADR-0007 rule 6: oxygen does not need 20 Hz");
    }

    @Test
    @DisplayName("nothing draining means unlimited air, not zero seconds")
    void noDrainIsUnlimited() {
        assertEquals(Integer.MAX_VALUE, AtmosphereTuning.secondsRemaining(80, 0.0f));
        assertEquals(Integer.MAX_VALUE, AtmosphereTuning.secondsRemaining(0, 0.0f));
        assertEquals(Integer.MAX_VALUE, AtmosphereTuning.secondsRemaining(80, -1.0f));
    }

    @Test
    @DisplayName("remaining seconds round down, so the HUD never promises time you do not have")
    void remainingRoundsDown() {
        assertEquals(2, AtmosphereTuning.secondsRemaining(9, 4.0f));
        assertEquals(0, AtmosphereTuning.secondsRemaining(3, 4.0f));
        assertEquals(0, AtmosphereTuning.secondsRemaining(0, 4.0f));
    }

    @Test
    @DisplayName("durations read as a player would say them")
    void durationFormatting() {
        assertEquals("--", AtmosphereTuning.formatDuration(Integer.MAX_VALUE));
        assertEquals("0s", AtmosphereTuning.formatDuration(0));
        assertEquals("59s", AtmosphereTuning.formatDuration(59));
        assertEquals("1m 0s", AtmosphereTuning.formatDuration(60));
        assertEquals("5m 0s", AtmosphereTuning.formatDuration(300));
        assertEquals("2m 5s", AtmosphereTuning.formatDuration(125));
    }

    /**
     * A single emitter must not be able to flood-fill the loaded world. The fill runs on block
     * changes, so an unbounded one is a server-freezing bug rather than a balance problem.
     */
    @Test
    @DisplayName("a sealed volume is bounded on both count and reach")
    void sealedVolumeIsBounded() {
        assertTrue(AtmosphereTuning.SEALED_VOLUME_LIMIT > 0);
        assertTrue(AtmosphereTuning.SEALED_VOLUME_RADIUS > 0);
        int cube = AtmosphereTuning.SEALED_VOLUME_RADIUS * 2 + 1;
        assertTrue(AtmosphereTuning.SEALED_VOLUME_LIMIT < cube * cube * cube,
                "the block limit must bite before the radius does, or it is not a limit");
    }
}
