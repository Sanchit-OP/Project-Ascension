package com.ascension.compat.sable.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A ship's heading must match the yaw convention ascension-worlds uses to place a player rising on
 * foot ({@code AscentDestination}: x = -sin(yaw), z = cos(yaw)), or ships and players leaving the
 * same way would come out in different places.
 */
class ShipHeadingTest {

    private static final double DELTA = 1e-4;

    @Test
    void flyingSouthIsYawZero() {
        assertEquals(0.0, ShipHeading.yaw(0.0, 5.0, 123.0F), DELTA);
    }

    @Test
    void flyingWestIsYaw90() {
        assertEquals(90.0, ShipHeading.yaw(-5.0, 0.0, 123.0F), DELTA);
    }

    @Test
    void flyingEastIsYawMinus90() {
        assertEquals(-90.0, ShipHeading.yaw(5.0, 0.0, 123.0F), DELTA);
    }

    @Test
    void flyingNorthIsYaw180() {
        assertEquals(180.0, Math.abs(ShipHeading.yaw(0.0, -5.0, 123.0F)), DELTA);
    }

    @Test
    void aShipClimbingStraightUpUsesTheFallback() {
        assertEquals(123.0, ShipHeading.yaw(0.3, -0.4, 123.0F), DELTA);
    }
}
