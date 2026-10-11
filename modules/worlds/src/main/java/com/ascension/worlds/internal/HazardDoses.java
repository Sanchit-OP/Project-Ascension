package com.ascension.worlds.internal;

/**
 * When a planet's {@link com.ascension.worlds.api.Planet.Hazard} doses a player, as a pure rule:
 * no player, no effect instance, so it is unit-testable and decided in one place.
 * {@link PlanetHazardMechanics} applies it once a second.
 *
 * <p>Why renew only near the end of a dose, rather than resetting it every check: a damage-over-
 * time effect like wither deals its damage when the dose's remaining time hits a multiple of its
 * own period. Resetting the dose every check would keep it from ever counting down that far, and
 * the rot would never bite.
 */
final class HazardDoses {

    /** How often players on a hazardous surface are checked, in ticks. */
    static final int CHECK_INTERVAL = 20;

    /** Vanilla's remaining-time value for an effect that never runs out. */
    static final int INFINITE = -1;

    enum Dose {
        /** Leave the player as they are. */
        NONE,
        /** The effect starts: dose them, and tell them why. */
        START,
        /** The effect is about to run out: top it up, quietly. */
        RENEW
    }

    private HazardDoses() {
    }

    /**
     * @param exempt    creative, spectator or dead: never dosed
     * @param warded    wearing something that keeps the hazard off
     * @param hasEffect already under the hazard's effect, from this or anything else
     * @param remaining ticks left on that effect; ignored when {@code hasEffect} is false
     */
    static Dose next(boolean exempt, boolean warded, boolean hasEffect, int remaining) {
        if (exempt || warded) {
            return Dose.NONE;
        }
        if (!hasEffect) {
            return Dose.START;
        }
        if (remaining == INFINITE) {
            return Dose.NONE;
        }
        // Renew if the dose would run out before the next check.
        return remaining <= CHECK_INTERVAL ? Dose.RENEW : Dose.NONE;
    }
}
