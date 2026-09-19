package com.ascension.combat.internal.mechanics;

/**
 * A stack counter that gains at most one stack per fixed interval while some caller-checked
 * condition holds (docs/technical/combat-api.md section 5) &mdash; The Protean Mask's resistance
 * stacks were the first shape of this (a stack every second in combat, up to a cap); Vanguard's
 * Conqueror's Resolve keystone is the second real consumer, which is what pulled this out of
 * {@code internal.armor} and into {@code internal.mechanics}, the same extraction {@link Stasis}
 * went through on its own second consumer.
 *
 * <p>Not {@link StackingChargeState}: that class grows on discrete hit events with a single
 * expiry: this grows on a fixed cadence gated by a boolean condition the caller re-checks every
 * tick, with no expiry of its own &mdash; the caller clears it explicitly once the condition
 * stops holding.
 *
 * <p>Deliberately no serialization: fight-local bookkeeping, not data anything should expect to
 * survive a world reload.
 */
public final class CombatStackState {

    private int stacks;
    private long nextStackGameTime;

    public int stacks() {
        return stacks;
    }

    /**
     * Call once per tick while the condition holds and the cap hasn't been reached. Returns
     * {@code true} exactly on the tick a new stack is gained, so the caller can react to reaching
     * the cap.
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
