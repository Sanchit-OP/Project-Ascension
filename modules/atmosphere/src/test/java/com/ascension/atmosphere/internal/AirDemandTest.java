package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.atmosphere.internal.sealed.AirDemand;
import org.junit.jupiter.api.Test;

class AirDemandTest {

    @Test
    void noRoomCostsNothing() {
        assertEquals(0, AirDemand.mbPerSecond(0));
    }

    @Test
    void smallestRoomCostsOne() {
        assertEquals(1, AirDemand.mbPerSecond(1));
        assertEquals(1, AirDemand.mbPerSecond(16));
        assertEquals(2, AirDemand.mbPerSecond(17));
    }

    @Test
    void biggerRoomsCostMore() {
        assertEquals(125, AirDemand.mbPerSecond(2000));
        assertTrue(AirDemand.mbPerSecond(4096) > AirDemand.mbPerSecond(2000));
    }

    @Test
    void aFullBufferOutlastsAShortStall() {
        // The biggest room still gets several seconds from one full dispenser.
        assertTrue(AtmosphereTuning.DISPENSER_CAPACITY_MB
                / AirDemand.mbPerSecond(AtmosphereTuning.SEALED_VOLUME_LIMIT) >= 10);
    }
}
