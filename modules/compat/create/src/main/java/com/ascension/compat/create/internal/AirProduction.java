package com.ascension.compat.create.internal;

/**
 * What a turning compressor makes, as plain arithmetic (no world, so it is unit tested).
 *
 * <p>One rule: 0.5 mB of air per second for every RPM it is turned at, in either direction. A
 * compressor at 256 RPM makes 128 mB/s, which holds a room of about 2,000 blocks (an Air
 * Dispenser spends 1 mB/s per 16 blocks). More speed, more air, a bigger room.
 */
public final class AirProduction {

    /** mB per second per RPM. */
    public static final float MB_PER_SECOND_PER_RPM = 0.5f;

    /** Stress units per RPM the machine adds to the network, shown by goggles. */
    public static final float STRESS_PER_RPM = 2.0f;

    /** Air the compressor can hold until a pump or a hand takes it, in mB. */
    public static final int BUFFER_MB = 4000;

    private AirProduction() {
    }

    /** mB made per game tick at this speed. */
    public static float mbPerTick(float rpm) {
        return Math.abs(rpm) * MB_PER_SECOND_PER_RPM / 20.0f;
    }

    /** mB per second at this speed, for display. */
    public static float mbPerSecond(float rpm) {
        return Math.abs(rpm) * MB_PER_SECOND_PER_RPM;
    }
}
