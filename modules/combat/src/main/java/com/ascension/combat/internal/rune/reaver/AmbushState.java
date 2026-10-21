package com.ascension.combat.internal.rune.reaver;

/**
 * Ambush's bookkeeping for one player (docs/progression/rune-audit.md): when they last landed a hit, and whether a dash, a
 * blink or an ender pearl has opened the way for a hit. A hit is <em>primed</em> when it is the first after a quiet spell, or the
 * first after the player moved in on their target.
 *
 * <p>Pure numbers, game ticks passed in, nothing held that could leak (ADR-0007); fight-local, not serialised.
 * Unit-tested ({@code AmbushStateTest}).
 */
public final class AmbushState {
    private long lastHitAt = Long.MIN_VALUE / 2;
    private long primedUntil = -1L;

    /** Whether a hit landing at {@code now} is primed: the player has been quiet for {@code idleTicks}, or has moved in lately. */
    public boolean primed(long now, long idleTicks) {
        return now < primedUntil || now - lastHitAt >= idleTicks;
    }

    /** A dash, blink or pearl: the next hit before {@code until} is primed. */
    public void prime(long until) {
        primedUntil = Math.max(primedUntil, until);
    }

    /** A hit landed: the quiet spell starts again and any priming is spent. */
    public void hit(long now) {
        lastHitAt = now;
        primedUntil = -1L;
    }

    /**
     * Ticks of quiet so far, up to {@code idleTicks}, for the HUD: it reads full once a hit would be primed (whether by quiet or by
     * a move), so the chip is a plain "ready" light.
     */
    public long readiness(long now, long idleTicks) {
        if (primed(now, idleTicks)) {
            return idleTicks;
        }
        return Math.max(0L, Math.min(idleTicks, now - lastHitAt));
    }
}
