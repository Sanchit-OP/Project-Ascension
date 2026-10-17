package com.ascension.combat.internal.rune.vanguard;

import java.util.UUID;

/**
 * Cadence's bookkeeping for one attacker: how many fully charged hits have landed on one enemy
 * inside the window (docs/technical/rune-system.md section 4.1). Which swing was fully charged is
 * {@link com.ascension.combat.internal.rune.SwingMarks}.
 *
 * <p>Pure counting, no entity and no world: an enemy is held by its {@link UUID}, and time is a
 * game tick passed in, so the rules are unit-tested ({@code CadenceStateTest}) and nothing here
 * can keep an entity alive (ADR-0007 rule 1). Not serialised: fight-local.
 */
public final class CadenceState {

    private UUID target;
    private int hits;
    private long windowEnds;

    /**
     * Counts a fully charged hit on {@code target}. The window opens with the first hit and all
     * the hits must land inside it; a hit on another enemy, or after the window, starts a new
     * count.
     *
     * @return whether this hit completed the count; if so the count has reset
     */
    public boolean hit(UUID target, long now, int hitsNeeded, long windowTicks) {
        if (hits == 0 || !target.equals(this.target) || now >= windowEnds) {
            this.target = target;
            this.hits = 0;
            this.windowEnds = now + windowTicks;
        }
        hits++;
        if (hits >= hitsNeeded) {
            clear();
            return true;
        }
        return false;
    }

    public int hits() {
        return hits;
    }

    public void clear() {
        hits = 0;
        target = null;
    }
}
