package com.ascension.combat.internal.rune.aetherwright;

/**
 * Arc's bookkeeping for one player: hits counted toward the charge, whether the weapon is charged now, and when the last bolt
 * left (docs/technical/rune-system.md section 4.3, reworked 2026-10-22). A few hits charge the weapon, and it then <b>stays charged
 * until the fight ends</b>: every hit after that throws a bolt. Using an item's active charges it at once.
 *
 * <p>A bolt may leave at most once every {@code gap} ticks, so a weapon that swings seven times a second does not throw seven bolts
 * of four strikes each.
 *
 * <p>Pure counting: unit-tested ({@code ArcStateTest}); not serialised, fight-local.
 */
public final class ArcState {
    private int hits;
    private boolean charged;
    private long lastBoltAt = Long.MIN_VALUE / 2;

    public boolean charged() {
        return charged;
    }

    /** Counts a weapon hit toward a charge; the {@code needed}th hit charges the weapon. */
    public void count(int needed) {
        if (++hits >= needed) {
            charged = true;
            hits = 0;
        }
    }

    /** Charges the weapon outright, as an item's active does. */
    public void charge() {
        charged = true;
        hits = 0;
    }

    /** Whether a bolt may leave now: the weapon is charged and the last one left at least {@code gap} ticks ago. */
    public boolean boltReady(long now, long gap) {
        return charged && now - lastBoltAt >= gap;
    }

    /** A bolt left at {@code now}. */
    public void bolt(long now) {
        lastBoltAt = now;
    }

    /** The fight is over: the charge is gone and the count starts again. */
    public void discharge() {
        charged = false;
        hits = 0;
    }

    public int hits() {
        return hits;
    }
}
