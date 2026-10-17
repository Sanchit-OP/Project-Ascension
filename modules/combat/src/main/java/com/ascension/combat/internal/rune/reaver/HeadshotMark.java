package com.ascension.combat.internal.rune.reaver;

/**
 * Deadeye's note on an arrow in flight: how much extra damage this arrow's last hit gets, as a
 * fraction, or 0 for a body shot. Set when the arrow reaches an enemy, read by the damage event
 * that follows in the same moment. Not serialised: an arrow's note dies with it.
 */
public final class HeadshotMark {

    private float bonus;

    public void set(float bonus) {
        this.bonus = bonus;
    }

    public float bonus() {
        return bonus;
    }
}
