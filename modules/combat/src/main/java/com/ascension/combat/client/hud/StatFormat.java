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

    /** Vanilla's bare-hand attack speed, in attacks per second. */
    static final double BARE_HAND_ATTACK_SPEED = 4.0;

    /**
     * Attack Speed against the bare hand: an empty hand reads 1.00, and a bonus reads above it
     * (+20% is 1.20). Shown this way because "4.00" for doing nothing special looks wrong.
     */
    static String attackSpeed(double perSecond) {
        return String.format(Locale.ROOT, "%.2f", perSecond / BARE_HAND_ATTACK_SPEED);
    }

    /**
     * Move Speed with vanilla's sprinting boost taken back out. Sprinting adds a transient +30%
     * on top of everything, which is not a bonus the player earned, so it must not show.
     *
     * @param sprintAmount the sprint modifier's amount (0.3), or 0 when it is not applied
     */
    static double withoutSprint(double value, double sprintAmount) {
        return sprintAmount == 0.0 ? value : value / (1.0 + sprintAmount);
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
