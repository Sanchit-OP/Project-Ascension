package com.ascension.combat.internal.mechanics;

/**
 * A generic "active until this game tick" duration window (docs/technical/combat-api.md section
 * 5) &mdash; the counterpart to {@link ArmedState} for a flag that clears itself once time has
 * passed, rather than one that sits and waits to be consumed. Wraithblade's post-blink speed
 * boost is the first consumer.
 */
public final class DurationState {

    private long expiresAtGameTime;

    /** The game tick it ends at, or 0 if it never started. For display; gameplay uses {@link #isActive}. */
    public long expiresAtGameTime() {
        return expiresAtGameTime;
    }

    public void activateUntil(long expiresAtGameTime) {
        this.expiresAtGameTime = expiresAtGameTime;
    }

    public boolean isActive(long now) {
        return now < expiresAtGameTime;
    }

    /**
     * Whether this was activated and has since lapsed, but not yet {@link #clear}ed &mdash; the
     * one tick a caller needs to notice the window just ended (Guardian's Wing's revive) rather
     * than merely no longer being active.
     */
    public boolean hasExpired(long now) {
        return expiresAtGameTime > 0L && now >= expiresAtGameTime;
    }

    public void clear() {
        this.expiresAtGameTime = 0L;
    }
}
