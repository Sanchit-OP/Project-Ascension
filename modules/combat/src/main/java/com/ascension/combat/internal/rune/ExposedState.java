package com.ascension.combat.internal.rune;

/**
 * Exposed, on one enemy: until a game tick, it takes {@code amplify} more damage from everyone
 * ({@link ExposedDamage}). Cadence applies it, with its own strength and length from its data.
 *
 * <p>Two sources never stack and never weaken each other: applying again keeps the stronger
 * strength and the later end, so a second, weaker Exposed can't cut a stronger one short. Pure
 * numbers, no entity: unit-tested ({@code ExposedStateTest}), not serialised.
 */
public final class ExposedState {

    private long until;
    private float amplify;

    /** Exposes until game tick {@code until}, taking {@code amplify} (0.10 is 10%) more damage. */
    public void expose(long now, long until, float amplify) {
        if (!isActive(now) || amplify > this.amplify) {
            this.amplify = amplify;
        }
        this.until = Math.max(this.until, until);
    }

    public boolean isActive(long now) {
        return now < until;
    }

    /** How much more damage is taken now, as a fraction; 0 when not exposed. */
    public float amplify(long now) {
        return isActive(now) ? amplify : 0.0f;
    }
}
