package com.ascension.atmosphere.api;

/**
 * What the air is like at a position.
 *
 * <p>Deliberately two fields. This is authored gameplay logic, not a gas simulation: a thin
 * atmosphere drains at {@code 0.5}, a corrosive one at {@code 2.0}, and that is the entire
 * model. Anything richer crosses the Authored Simulation Boundary in
 * {@code docs/gameplay/oxygen.md}.
 *
 * @param breathable      whether a player can breathe here without a supply
 * @param drainMultiplier how fast a supply is consumed here; ignored when {@code breathable}
 */
public record Atmosphere(boolean breathable, float drainMultiplier) {

    /** Ordinary breathable air. Costs nothing. */
    public static final Atmosphere BREATHABLE = new Atmosphere(true, 0.0f);

    /** No air at all. Baseline drain. */
    public static final Atmosphere VACUUM = new Atmosphere(false, 1.0f);

    public Atmosphere {
        if (drainMultiplier < 0.0f || !Float.isFinite(drainMultiplier)) {
            throw new IllegalArgumentException(
                    "drainMultiplier must be finite and non-negative, got " + drainMultiplier);
        }
    }

    /** Unbreathable air that drains at the given rate. */
    public static Atmosphere hostile(float drainMultiplier) {
        return new Atmosphere(false, drainMultiplier);
    }
}
