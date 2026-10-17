package com.ascension.combat.internal.rune.aetherwright;

/**
 * Arc's bookkeeping for one player: hits counted toward the next charge, and whether the weapon is
 * charged now (docs/technical/rune-system.md section 4.3). Every few hits charge it; the next hit
 * releases the lightning and starts the count again; using an item's active charges it outright.
 *
 * <p>Pure counting: unit-tested ({@code ArcStateTest}); not serialised, fight-local.
 */
public final class ArcState {

    private int hits;
    private boolean charged;

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

    /** The weapon released its lightning: charge spent, count starts again. */
    public void release() {
        charged = false;
        hits = 0;
    }

    public int hits() {
        return hits;
    }
}
