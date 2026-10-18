package com.ascension.combat.internal.rune.bulwark;

/**
 * Plating's bookkeeping for one player (docs/technical/rune-system.md section 4.4): a hard hit
 * starts a short window in which the next few hits are softened, then a cooldown must pass before
 * another hard hit can start one. The hit that starts it is not itself softened.
 *
 * <p>Pure numbers: unit-tested ({@code BulwarkTest}); not serialised, fight-local.
 */
public final class PlatingState {

    private long cooldownUntil;
    private int hitsLeft;
    private long windowEnds;

    /** Whether hits are being softened now. */
    /** The game tick it can start again. For display. */
    public long cooldownUntil() {
        return cooldownUntil;
    }

    /** The game tick its window closes. For display. */
    public long windowEnds() {
        return windowEnds;
    }

    public boolean active(long now) {
        return hitsLeft > 0 && now < windowEnds;
    }

    /** Whether a hard hit may start a window now. */
    public boolean ready(long now) {
        return now >= cooldownUntil && !active(now);
    }

    public void start(long now, int hits, long windowTicks, long cooldownTicks) {
        hitsLeft = hits;
        windowEnds = now + windowTicks;
        cooldownUntil = now + cooldownTicks;
    }

    /** A hit was softened. */
    public void consume() {
        hitsLeft--;
    }

    public int hitsLeft() {
        return hitsLeft;
    }
}
