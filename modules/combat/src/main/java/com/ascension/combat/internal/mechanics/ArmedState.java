package com.ascension.combat.internal.mechanics;

/**
 * A generic "armed until consumed" flag, no expiry (docs/technical/combat-api.md section 5) --
 * unlike {@link StackingChargeState}, nothing about this decays on its own, since a duration
 * doesn't make sense for a charge that's meant to sit and wait for the next qualifying hit.
 * Divine Sunderer's primed-next-attack and Darken Blade's alternating-hit toggle are the same
 * shape: set on one hit, read and cleared on a later one.
 */
public final class ArmedState {

    private boolean armed;

    public void arm() {
        armed = true;
    }

    /**
     * Reads the flag without clearing it. Some consumers (Divine Sunderer) need to know whether
     * to act on an earlier stage of the same hit than the one that actually consumes the charge
     * -- see the consuming weapon's own effects class for why.
     */
    public boolean isArmed() {
        return armed;
    }

    /** Clears the flag and reports whether it was set. */
    public boolean consume() {
        boolean wasArmed = armed;
        armed = false;
        return wasArmed;
    }
}
