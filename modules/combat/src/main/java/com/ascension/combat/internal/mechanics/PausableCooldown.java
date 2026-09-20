package com.ascension.combat.internal.mechanics;

/**
 * A countdown that only progresses while a caller-checked condition holds (docs/technical/combat-
 * api.md section 5) &mdash; unlike vanilla {@code ItemCooldowns}, which ticks down unconditionally
 * once started, regardless of whether the item is even worn or held. Guardian's Wing is the first
 * consumer: Sanchit's own spec was that a death-save's cooldown should only count against a player
 * who is actually wearing the risk, not tick down quietly in a pocket while they carry the item
 * unequipped and effectively get an early recharge.
 *
 * <p>Deliberately ticks in whole-number remaining ticks rather than an absolute "ready at this
 * game time" deadline (the shape every other cooldown/duration in this catalog uses) &mdash; a
 * deadline doesn't make sense for something that can pause, since "ready at tick X" stops meaning
 * anything the moment progress halts partway there.
 */
public final class PausableCooldown {

    private int remainingTicks;

    public void start(int totalTicks) {
        this.remainingTicks = totalTicks;
    }

    public boolean isReady() {
        return remainingTicks <= 0;
    }

    /** Call once per tick; {@code progressing} is whatever condition should count against the cooldown. */
    public void tick(boolean progressing) {
        if (progressing && remainingTicks > 0) {
            remainingTicks--;
        }
    }
}
