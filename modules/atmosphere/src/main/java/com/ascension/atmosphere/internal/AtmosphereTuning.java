package com.ascension.atmosphere.internal;

/**
 * Every tunable number in one place.
 *
 * <p>Kept together so balance changes are a single diff rather than a hunt, and so the whole
 * model stays small enough to read at once &mdash; {@code docs/gameplay/oxygen.md} treats a
 * model too granular to communicate as overdesigned.
 */
public final class AtmosphereTuning {

    /**
     * Ticks between oxygen accounting passes.
     *
     * <p>Chosen deliberately per ADR-0007 rule 6. Oxygen does not need 20 Hz; twice a second is
     * imperceptible against a multi-second failure window, and costs a twentieth of the work.
     */
    public static final int ACCOUNTING_INTERVAL_TICKS = 10;

    /**
     * Forced resync interval, independent of change detection.
     *
     * <p>Sync is normally driven by change alone. This is the low-frequency reconciliation that
     * repairs a client which missed a packet, without turning sync into a stream.
     */
    public static final int RECONCILE_INTERVAL_TICKS = 100;

    /** Units drained per second in a baseline unbreathable atmosphere, before modifiers. */
    public static final int BASE_UNITS_PER_SECOND = 4;

    /** Units in one standard portable tank. At baseline drain, five minutes of air. */
    public static final int TANK_CAPACITY = BASE_UNITS_PER_SECOND * 60 * 5;

    /**
     * Ticks between running out of air and taking the first damage.
     *
     * <p>The "short failure window" from {@code docs/gameplay/oxygen.md}: long enough to turn
     * and run for a door, short enough that ignoring the bar kills you.
     */
    public static final int SUFFOCATION_GRACE_TICKS = 40;

    /** Damage applied per second once the grace window has elapsed. Matches vanilla drowning. */
    public static final float SUFFOCATION_DAMAGE = 2.0f;

    private AtmosphereTuning() {
    }

    /** Seconds covered by one accounting pass. */
    public static float accountingSeconds() {
        return ACCOUNTING_INTERVAL_TICKS / 20.0f;
    }

    /**
     * Seconds of air remaining at the given drain rate.
     *
     * @return {@link Integer#MAX_VALUE} when nothing is draining
     */
    public static int secondsRemaining(int units, float drainPerSecond) {
        if (drainPerSecond <= 0.0f) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(units / drainPerSecond);
    }
}
