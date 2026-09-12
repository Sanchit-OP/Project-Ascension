package com.ascension.combat.internal.mechanics;

/**
 * A generic "hits build a stack, stack expires if the next hit doesn't come in time" counter
 * (docs/technical/combat-api.md section 5) &mdash; Darius's Axe's armor shred and Spear of
 * Shojin's combo counter are the same shape (a count and an expiry) with different payoffs, so
 * the bookkeeping is shared and only the payoff lives in each weapon's own effects class.
 *
 * <p>Deliberately no serialization: this is fight-local bookkeeping, not data anything should
 * expect to survive a world reload.
 */
public final class StackingChargeState {

    private int stacks;
    private long expiresAtGameTime;

    public int stacks() {
        return stacks;
    }

    public long expiresAtGameTime() {
        return expiresAtGameTime;
    }

    public void refresh(int maxStacks, long expiresAtGameTime) {
        this.stacks = Math.min(maxStacks, this.stacks + 1);
        this.expiresAtGameTime = expiresAtGameTime;
    }

    public void clear() {
        this.stacks = 0;
    }
}
