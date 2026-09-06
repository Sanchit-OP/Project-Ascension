package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * The trig behind placing an ascending player past their departure planet's approach shell,
 * rather than exactly on top of it. See {@code SpaceMechanics.checkAscent}'s comment for the bug
 * this geometry fixes (arriving at distance 0 from your own shell self-triggered an immediate
 * return descent).
 */
class AscentDestinationTest {

    private static final double DELTA = 1e-9;

    @Test
    void yawZeroPointsTowardPositiveZ() {
        // Vanilla yaw 0 faces south, +Z. Offsetting from the origin should land purely on +Z.
        Vec3 destination = AscentDestination.compute(0.0, 0.0, 0.0, 100.0);
        assertEquals(0.0, destination.x(), DELTA);
        assertEquals(100.0, destination.z(), DELTA);
    }

    @Test
    void yaw90PointsTowardNegativeX() {
        Vec3 destination = AscentDestination.compute(0.0, 0.0, 90.0, 100.0);
        assertEquals(-100.0, destination.x(), DELTA);
        assertEquals(0.0, destination.z(), DELTA);
    }

    @Test
    void yaw180PointsTowardNegativeZ() {
        Vec3 destination = AscentDestination.compute(0.0, 0.0, 180.0, 100.0);
        assertEquals(0.0, destination.x(), DELTA);
        assertEquals(-100.0, destination.z(), DELTA);
    }

    @Test
    void offsetIsAppliedRelativeToThePlanetsOwnPosition() {
        Vec3 destination = AscentDestination.compute(8000.0, 1500.0, 0.0, 50.0);
        assertEquals(8000.0, destination.x(), DELTA);
        assertEquals(1550.0, destination.z(), DELTA);
    }

    @Test
    void destinationAlwaysSitsOnTheSharedPlanetaryPlane() {
        Vec3 destination = AscentDestination.compute(123.0, -456.0, 37.0, 612.0);
        assertEquals(0.0, destination.y(), DELTA);
    }
}
