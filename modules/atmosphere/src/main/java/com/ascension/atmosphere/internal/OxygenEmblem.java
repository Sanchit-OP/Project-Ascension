package com.ascension.atmosphere.internal;

/**
 * What the oxygen emblem on the HUD should be doing, decided from the player's air. Pure, so a
 * unit test decides it (ADR-0008); how it looks is checked in a client.
 *
 * <p>The rule is that oxygen says nothing when air is fine, rises into view as it drops, and gets
 * loud only when it matters: nothing, then a quiet badge, then the big emblem, then the emblem with
 * a red screen edge.
 */
public final class OxygenEmblem {

    /** What to draw. */
    public enum Mode {
        /** Breathable air and full lungs: draw nothing. */
        HIDDEN,
        /** A small quiet badge: air is fine, or climbing back in breathable air. */
        QUIET,
        /** The big emblem: air is running down. */
        WARNING,
        /** The big emblem, red and pulsing, with the screen edge reddening. */
        CRITICAL
    }

    /** Below this share of capacity, in unbreathable air, the big emblem shows even with time left. */
    static final float WARNING_SHARE = 0.5f;

    private OxygenEmblem() {
    }

    /**
     * @param shouldRender     whether there is anything worth drawing at all (the HUD's own gate)
     * @param breathable       whether the surrounding air is breathable
     * @param suffocating      inside the failure window
     * @param pressurising     a tank is coming up to pressure
     * @param units            air held
     * @param capacity         air the player could hold
     * @param secondsRemaining seconds of air left at the current drain
     */
    public static Mode modeFor(boolean shouldRender, boolean breathable, boolean suffocating,
                               boolean pressurising, int units, int capacity, int secondsRemaining) {
        if (!shouldRender) {
            return Mode.HIDDEN;
        }
        if (suffocating) {
            return Mode.CRITICAL;
        }
        if (breathable) {
            // Refilling in good air: show it climbing, quietly.
            return Mode.QUIET;
        }
        if (OxygenLevel.forSecondsRemaining(secondsRemaining) == OxygenLevel.CRITICAL) {
            return Mode.CRITICAL;
        }
        if (pressurising
                || OxygenLevel.forSecondsRemaining(secondsRemaining) == OxygenLevel.LOW
                || share(units, capacity) < WARNING_SHARE) {
            return Mode.WARNING;
        }
        return Mode.QUIET;
    }

    /** Air held as a share of capacity, 0 to 1. */
    public static float share(int units, int capacity) {
        if (capacity <= 0) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float) units / capacity));
    }
}
