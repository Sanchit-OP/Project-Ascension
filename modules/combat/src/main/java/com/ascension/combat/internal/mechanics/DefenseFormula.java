package com.ascension.combat.internal.mechanics;

/**
 * The one curve both defensive stats share (docs/technical/combat-api.md section 3), Sanchit's
 * own spec, 2026-09-19: each 100 points halves the damage that gets through, rather than League's
 * real {@code armor / (100 + armor)} curve. 100 -> 50% reduction, 200 -> 75%, 300 -> 87.5%, and so
 * on without ever reaching a hard cap. {@link com.ascension.combat.internal.mechanics.PhysicalDamage}
 * and {@link MagicDamage} both apply this to Armor and Magic Resist respectively &mdash; one
 * formula, so the two stats can never quietly drift apart.
 */
public final class DefenseFormula {

    private DefenseFormula() {
    }

    /** The fraction of damage that gets through a given amount of the defensive stat. */
    public static double multiplier(double stat) {
        return Math.pow(0.5, stat / 100.0);
    }
}
