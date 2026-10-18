package com.ascension.combat.client.hud;

import java.util.Locale;

/**
 * How a stat is written on the HUD: short, the way a player says it. Pure, so a unit test decides
 * it. Values are in the attribute's own units ({@code StatReader}).
 */
final class StatFormat {

    /** A player walks at 0.1 in vanilla's units; Move Speed is shown as the bonus over that. */
    static final double WALK_SPEED = 0.1;

    private StatFormat() {
    }

    /** Points: whole numbers, with one decimal only for small fractional values (a bare hand's 1). */
    static String points(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value) >= 100.0 || Math.abs(value - rounded) < 0.05) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Attacks per second, two decimals. */
    static String attackSpeed(double perSecond) {
        return String.format(Locale.ROOT, "%.2f", perSecond);
    }

    /** A fraction as a percent: 0.08 is "8%". */
    static String percent(double fraction) {
        return Math.round(fraction * 100.0) + "%";
    }

    /** Move Speed as the bonus over walking: "+10%", "0%", "-15%". */
    static String moveSpeed(double value) {
        long bonus = Math.round((value / WALK_SPEED - 1.0) * 100.0);
        return bonus > 0 ? "+" + bonus + "%" : bonus + "%";
    }
}
