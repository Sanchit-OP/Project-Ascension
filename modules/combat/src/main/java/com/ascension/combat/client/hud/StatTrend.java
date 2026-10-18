package com.ascension.combat.client.hud;

/**
 * Remembers whether a stat has just gone up or down, so the HUD can colour its number green or
 * red for a few seconds after a buff or debuff moves it. Pure; holds numbers only.
 *
 * <p>The first moments after joining are ignored: stats arrive from the server over a few ticks
 * and would otherwise flash green on every login.
 */
final class StatTrend {

    /** Ticks a change stays coloured: three seconds. */
    static final int HOLD_TICKS = 60;

    /** Ticks after a reset during which changes are taken in silently. */
    static final int SETTLE_TICKS = 40;

    private double last = Double.NaN;
    private int direction;
    private long until;
    private long armedAt;

    void update(double value, long tick) {
        if (Double.isNaN(last)) {
            last = value;
            armedAt = tick + SETTLE_TICKS;
            return;
        }
        if (Math.abs(value - last) > 1.0e-6) {
            if (tick >= armedAt) {
                direction = value > last ? 1 : -1;
                until = tick + HOLD_TICKS;
            }
            last = value;
        }
    }

    /** 1 while a rise is recent, -1 while a fall is, otherwise 0. */
    int direction(long tick) {
        return tick < until ? direction : 0;
    }

    void reset() {
        last = Double.NaN;
        direction = 0;
        until = 0;
        armedAt = 0;
    }
}
