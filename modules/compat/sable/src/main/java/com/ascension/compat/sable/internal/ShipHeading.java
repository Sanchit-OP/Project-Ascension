package com.ascension.compat.sable.internal;

/**
 * Which way a ship is going, as Minecraft yaw (degrees, 0 = south, 90 = west), for placing it in
 * space along the line it was flying when it left its planet's sky.
 */
final class ShipHeading {

    /**
     * Below this speed across the ground, in blocks per second, a ship is climbing more than going
     * anywhere, and its drift says nothing about where it was meant to go.
     */
    static final double MIN_SPEED = 1.0;

    private ShipHeading() {
    }

    /** The heading of horizontal velocity {@code vx, vz}, or {@code fallback} when it is too slow to tell. */
    static float yaw(double vx, double vz, float fallback) {
        if (vx * vx + vz * vz < MIN_SPEED * MIN_SPEED) {
            return fallback;
        }
        return (float) Math.toDegrees(Math.atan2(-vx, vz));
    }
}
