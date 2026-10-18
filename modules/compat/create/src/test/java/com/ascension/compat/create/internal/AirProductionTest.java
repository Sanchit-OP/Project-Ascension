package com.ascension.compat.create.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AirProductionTest {

    @Test
    void stoppedMakesNothing() {
        assertEquals(0f, AirProduction.mbPerTick(0f));
    }

    @Test
    void outputFollowsSpeedInEitherDirection() {
        assertEquals(128f, AirProduction.mbPerSecond(256f), 1e-4);
        assertEquals(AirProduction.mbPerSecond(64f), AirProduction.mbPerSecond(-64f), 1e-4);
        assertEquals(AirProduction.mbPerSecond(128f) * 2, AirProduction.mbPerSecond(256f), 1e-4);
    }

    @Test
    void aTickIsATwentiethOfASecond() {
        assertEquals(AirProduction.mbPerSecond(100f), AirProduction.mbPerTick(100f) * 20, 1e-4);
    }

    @Test
    void oneFastCompressorHoldsAHall() {
        // 128 mB/s against 1 mB/s per 16 blocks of room: about 2,000 blocks.
        assertTrue(AirProduction.mbPerSecond(256f) * 16 >= 2000);
    }
}
