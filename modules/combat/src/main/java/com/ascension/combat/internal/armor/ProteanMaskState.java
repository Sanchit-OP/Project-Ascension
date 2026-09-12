package com.ascension.combat.internal.armor;

/**
 * The Protean Mask's resistance-stack counter (docs/technical/combat-api.md section 6): a stack
 * every second while in combat, up to a cap. Not {@link com.ascension.combat.internal.mechanics.StackingChargeState}
 * &mdash; that class grows on discrete hit events with a single expiry; this grows on a fixed
 * cadence gated by a boolean condition (in combat or not) rather than any one hit, so it needed
 * its own shape. Only one consumer so far, so this stays local to the armor package rather than
 * moving to {@code internal.mechanics}.
 */
public final class ProteanMaskState {

    private int stacks;
    private long nextStackGameTime;

    public int stacks() {
        return stacks;
    }

    /**
     * Call once per tick while the wearer is in combat and below the cap. Returns {@code true}
     * exactly on the tick a new stack is gained, so the caller can react to reaching the cap.
     */
    public boolean tryGainStack(long now, int maxStacks, long intervalTicks) {
        if (stacks >= maxStacks) {
            return false;
        }

        if (nextStackGameTime == 0L) {
            nextStackGameTime = now + intervalTicks;
            return false;
        }

        if (now < nextStackGameTime) {
            return false;
        }

        stacks++;
        nextStackGameTime = now + intervalTicks;
        return true;
    }

    public void clear() {
        stacks = 0;
        nextStackGameTime = 0L;
    }
}
