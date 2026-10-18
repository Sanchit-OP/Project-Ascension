package com.ascension.hud.internal;

/**
 * How much health a regeneration effect is about to give back. Pure arithmetic.
 *
 * <p>Only what the game can actually predict is shown (the grey part of the health bar): a
 * Regeneration effect heals 1 health every {@code 50 >> amplifier} ticks for as long as it lasts.
 * Natural regeneration from a full stomach has no end to count to, so it is not forecast.
 *
 * <p>The forecast looks no further ahead than {@link #HORIZON_TICKS}, so a long potion does not
 * paint the whole bar grey.
 */
public final class RegenForecast {

    /** Five seconds. */
    public static final int HORIZON_TICKS = 100;

    private RegenForecast() {
    }

    /**
     * @param ticksLeft remaining duration of the effect; zero or less means none
     * @param amplifier the effect's amplifier, 0 for level I
     * @return health to be restored within the horizon, never negative
     */
    public static float pending(int ticksLeft, int amplifier) {
        if (ticksLeft <= 0) {
            return 0.0f;
        }
        int interval = Math.max(1, 50 >> Math.min(Math.max(amplifier, 0), 30));
        int window = Math.min(ticksLeft, HORIZON_TICKS);
        return window / interval;
    }
}
