package com.ascension.combat.internal.rune.bulwark;

/**
 * Brace's bookkeeping for one player: when it may be triggered again, and whether they are braced
 * now (docs/technical/rune-system.md section 4.4). A brace lasts a moment, ends in a shockwave on
 * the player's own next tick, and the cooldown runs from the moment it began.
 *
 * <p>Pure numbers: unit-tested ({@code BulwarkTest}); not serialised, fight-local.
 */
public final class BraceState {

    private long cooldownUntil;
    private long endsAt = -1L;

    /** Whether a brace may begin: the cooldown is over and none is under way. */
    /** The game tick it can be started again. For display. */
    public long cooldownUntil() {
        return cooldownUntil;
    }

    public boolean ready(long now) {
        return now >= cooldownUntil && endsAt < 0L;
    }

    public void start(long now, long durationTicks, long cooldownTicks) {
        endsAt = now + durationTicks;
        cooldownUntil = now + cooldownTicks;
    }

    public boolean bracing() {
        return endsAt >= 0L;
    }

    /** Whether the brace that is under way has run its length. */
    public boolean over(long now) {
        return endsAt >= 0L && now >= endsAt;
    }

    public void end() {
        endsAt = -1L;
    }
}
