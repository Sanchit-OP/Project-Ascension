package com.ascension.combat.internal.rune;

import java.util.UUID;

/**
 * What a player's last melee swing was, noted at the start of the attack for the damage event that
 * follows it in the same tick (docs/technical/rune-system.md section 4): fully charged, and
 * whether it landed as a critical hit. A melee swing's charge is only known when the attack
 * begins (the damage event comes after its cooldown has reset), so {@link SwingTracking} reads it
 * there and the runes that care (Cadence, Ambush, Starfall) ask here.
 *
 * <p>Pure bookkeeping, no entity and no world: the target is a {@link UUID}, time is a game tick
 * passed in, and a mark is only good for the tick and the enemy it was made for. Unit-tested
 * ({@code SwingMarksTest}); not serialised.
 */
public final class SwingMarks {

    private UUID target;
    private long tick = -1L;
    private boolean charged;
    private boolean crit;

    /** A swing at {@code target} begins this tick; it forgets the swing before. */
    public void swing(UUID target, long now, boolean charged) {
        this.target = target;
        this.tick = now;
        this.charged = charged;
        this.crit = false;
    }

    /** The swing at {@code target} turned out a critical hit. */
    public void markCrit(UUID target, long now) {
        if (!isFor(target, now)) {
            swing(target, now, false);
        }
        crit = true;
    }

    /** Whether the swing that hit {@code target} this tick was fully charged. */
    public boolean charged(UUID target, long now) {
        return isFor(target, now) && charged;
    }

    /** Whether the swing that hit {@code target} this tick was a critical hit. */
    public boolean crit(UUID target, long now) {
        return isFor(target, now) && crit;
    }

    private boolean isFor(UUID target, long now) {
        return tick == now && target.equals(this.target);
    }
}
