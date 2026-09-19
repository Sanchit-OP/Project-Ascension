package com.ascension.combat.internal.mobs;

/**
 * A mob's place in this module's stat model (docs/technical/combat-api.md section 3) &mdash; a
 * handful of shared archetypes rather than bespoke per-species numbers, Sanchit's call: fast to
 * stand up, easy to rebalance broadly.
 *
 * <p>Carries both Armor and Magic Resist, both under the same {@link
 * com.ascension.combat.internal.mechanics.DefenseFormula} curve (100 points halves incoming
 * damage). Armor always outranks Magic Resist within a tier, per Sanchit's own correction
 * 2026-09-19: physical damage is the common case, magic-dealing weapons are the rarer, smaller
 * slice of the catalog (section 2), so wildlife should read that way too. Values reach into the
 * triple digits for the tougher tiers deliberately &mdash; under this curve, 100 is the point
 * where a stat starts to really matter (50% reduction), not a cap the way vanilla's old 0-30 Armor
 * range was.
 */
public enum MobStatTier {
    /** Fodder -- no meaningful resistance. */
    SWARM(0.0, 0.0),
    /** The common early threats. */
    LIGHT(10.0, 0.0),
    /** Genuinely dangerous, not yet a boss fight. */
    MEDIUM(25.0, 10.0),
    /** Built to tank a fight. */
    HEAVY(50.0, 20.0),
    /** The real end-of-fight encounters. */
    BOSS(100.0, 40.0);

    private final double armor;
    private final double magicResist;

    MobStatTier(double armor, double magicResist) {
        this.armor = armor;
        this.magicResist = magicResist;
    }

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    public double armor() {
        return armor;
    }

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    public double magicResist() {
        return magicResist;
    }
}
