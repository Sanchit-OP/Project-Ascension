package com.ascension.atmosphere.internal.supply;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import org.junit.jupiter.api.Test;

/** How much air one share moves from a tank into a teammate's lungs. */
class AirSharingTest {

    private static final int LUNGS = AtmosphereTuning.LUNG_CAPACITY;
    private static final int TANK = AtmosphereTuning.TANK_CAPACITY;

    @Test
    void fillsEmptyLungsFromAFullTank() {
        assertEquals(LUNGS, AirSharing.unitsToShare(TANK, 0, LUNGS));
    }

    @Test
    void onlyTopsUpTheMissingPart() {
        assertEquals(30, AirSharing.unitsToShare(TANK, LUNGS - 30, LUNGS));
    }

    @Test
    void anAlmostEmptyTankGivesWhatItHas() {
        assertEquals(5, AirSharing.unitsToShare(5, 0, LUNGS));
    }

    @Test
    void gearGrownLungsTakeMore() {
        int enchanted = LUNGS * 2;
        assertEquals(enchanted, AirSharing.unitsToShare(TANK, 0, enchanted));
    }

    @Test
    void fullLungsOrAnEmptyTankMoveNothing() {
        assertEquals(0, AirSharing.unitsToShare(TANK, LUNGS, LUNGS));
        assertEquals(0, AirSharing.unitsToShare(0, 0, LUNGS));
    }

    @Test
    void lungsAboveAShrunkCapacityNeverGoNegative() {
        // Taking off lung gear lowers capacity before the next pass clamps the reserve down.
        assertEquals(0, AirSharing.unitsToShare(TANK, LUNGS * 2, LUNGS));
    }
}
