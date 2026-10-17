package com.ascension.combat.internal.rune.vanguard;

import java.util.UUID;

/**
 * Cadence's bookkeeping for one attacker: how many fully charged hits have landed on one enemy
 * inside the window, and which swing was fully charged (docs/technical/rune-system.md section 4.1).
 *
 * <p>Pure counting, no entity and no world: an enemy is held by its {@link UUID}, and time is a
 * game tick passed in, so the rules are unit-tested ({@code CadenceStateTest}) and nothing here
 * can keep an entity alive (ADR-0007 rule 1). Not serialised: fight-local.
 */
public final class CadenceState {

    private UUID chargedTarget;
    private long chargedAt = -1L;

    private UUID target;
    private int hits;
    private long windowEnds;

    /**
     * Notes that this tick's swing at {@code target} was fully charged. A melee swing's charge is
     * only known at the start of the attack (the damage event comes after the cooldown has reset),
     * so the attack event records it and the damage event asks {@link #wasChargedSwing}.
     */
    public void markChargedSwing(UUID target, long now) {
        this.chargedTarget = target;
        this.chargedAt = now;
    }

    /** Whether the swing that hit {@code target} this tick was the fully charged one marked. */
    public boolean wasChargedSwing(UUID target, long now) {
        return chargedAt == now && target.equals(chargedTarget);
    }

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
