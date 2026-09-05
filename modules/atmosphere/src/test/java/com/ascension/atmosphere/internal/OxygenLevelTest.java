package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The single threshold table shared by the tank's durability bar and the HUD.
 *
 * <p>Written after finding the two had drifted into separate copies of the same two numbers
 * during the M2.4 pass &mdash; not because they disagreed yet, but because nothing stopped them
 * from starting to. This is what stops it happening again.
 */
class OxygenLevelTest {

    @Test
    void unlimitedAirReadsOk() {
        assertEquals(OxygenLevel.OK, OxygenLevel.forSecondsRemaining(Integer.MAX_VALUE));
    }

    @Test
    void aboveThirtySecondsIsOk() {
        assertEquals(OxygenLevel.OK, OxygenLevel.forSecondsRemaining(31));
    }

    @Test
    void thirtySecondsIsTheLowBoundary() {
        assertEquals(OxygenLevel.LOW, OxygenLevel.forSecondsRemaining(30));
        assertEquals(OxygenLevel.LOW, OxygenLevel.forSecondsRemaining(11));
    }

    @Test
    void tenSecondsIsTheCriticalBoundary() {
        assertEquals(OxygenLevel.CRITICAL, OxygenLevel.forSecondsRemaining(10));
        assertEquals(OxygenLevel.CRITICAL, OxygenLevel.forSecondsRemaining(1));
        assertEquals(OxygenLevel.CRITICAL, OxygenLevel.forSecondsRemaining(0));
    }
}
