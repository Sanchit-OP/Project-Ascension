package com.ascension.atmosphere.internal;

/**
 * The arithmetic of an accounting pass, with no Minecraft in it.
 *
 * <p>Extracted in the M2.4 refactor pass. Every number here decides how fast a player dies, and
 * all of it used to live inside methods that took a {@code ServerPlayer} &mdash; which meant the
 * only way to check any of it was to stand on the Moon and count. ADR-0008 is right that a unit
 * test proves nothing about world state, but none of this <em>is</em> world state: it is
 * subtraction, and subtraction can be checked cheaply and exactly.
 *
 * <p>The split is deliberately drawn at the Minecraft boundary. {@link OxygenTracker} keeps
 * everything that reads or writes a player; this keeps everything that is just numbers. Anything
 * that needs a {@code ServerPlayer} to answer does not belong here.
 */
final class OxygenAccounting {

    private OxygenAccounting() {
    }

    /**
     * What one pass costs: whole units to take now, and the fraction to carry forward.
     *
     * <p>Drain is a rate per second and a pass is half a second, so most passes owe a fractional
     * amount. Rounding each pass to a whole unit would make a drain of {@code 0.3} either free or
     * four times too expensive; carrying the remainder is what lets a modifier be a real
     * multiplier rather than a hint.
     *
     * @param units whole units owed this pass, never negative
     * @param carry the leftover fraction, always in {@code [0, 1)}
     */
    record Debt(int units, float carry) {
    }

    private static final Debt NOTHING = new Debt(0, 0.0f);

    /**
     * Work out this pass's debt.
     *
     * <p>Nothing draining clears the carry rather than preserving it: a player who reaches air
     * should not owe a fraction of a unit from a previous dive the next time they leave it.
     */
    static Debt debt(float drainPerSecond, float carriedFraction) {
        if (drainPerSecond <= 0.0f) {
            return NOTHING;
        }
        float owed = drainPerSecond * AtmosphereTuning.accountingSeconds() + carriedFraction;
        int whole = (int) owed;
        return new Debt(whole, owed - whole);
    }

    /**
     * Lungs after one pass of breathing free air.
     *
     * <p>Lungs are the one supply that refills for nothing, exactly like vanilla air bubbles.
     * Tanks deliberately do not: they are a resource you plan around and refill at a station,
     * which is what makes carrying one a decision.
     *
     * <p>Always gains at least one unit when below capacity. A refill rate that rounds to zero
     * per pass would leave a player stuck one unit short of full forever, permanently failing the
     * "lungs are full" fast path and resyncing twice a second for the rest of the session.
     *
     * <p>Over-full lungs are left alone rather than trimmed here. Trimming is what the caller does
     * when gear is removed mid-dive, and doing it in the refill path too would mean a player
     * losing capacity gets it taken twice.
     */
    static int lungsAfterRefill(int current, int capacity) {
        if (current >= capacity) {
            return current;
        }
        int gained = Math.round(
                AtmosphereTuning.LUNG_REFILL_PER_SECOND * AtmosphereTuning.accountingSeconds());
        return Math.min(capacity, current + Math.max(1, gained));
    }

    /**
     * How long a player has been out of air after this pass.
     *
     * <p>Resets to zero the moment any air is available, so a tank swap completed inside the
     * grace window costs nothing. That is the whole reason the window exists.
     */
    static int suffocationTicksAfter(int current, boolean hasAir) {
        return hasAir ? 0 : current + AtmosphereTuning.ACCOUNTING_INTERVAL_TICKS;
    }

    /**
     * Whether the grace window has elapsed and damage starts.
     *
     * <p>Strictly greater than, not "at least": a player is warned for the whole grace window and
     * hurt afterwards. At exactly the boundary they have used up the warning and not yet earned
     * the damage.
     */
    static boolean damageDue(int suffocationTicks) {
        return suffocationTicks > AtmosphereTuning.SUFFOCATION_GRACE_TICKS;
    }

    /** Damage for one pass, scaled from the per-second rate. */
    static float damagePerPass() {
        return AtmosphereTuning.SUFFOCATION_DAMAGE * AtmosphereTuning.accountingSeconds();
    }
}
