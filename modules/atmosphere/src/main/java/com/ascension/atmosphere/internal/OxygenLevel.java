package com.ascension.atmosphere.internal;

/**
 * How urgent a given amount of remaining air is, as a single source of truth for "when does this
 * turn yellow, and when does it turn red".
 *
 * <p>Extracted in the M2.4 pass. Before this, {@code OxygenTankItem}'s durability bar and
 * {@code OxygenHudLayer}'s status bar each carried their own copy of {@code SECONDS_LOW = 30} and
 * {@code SECONDS_CRITICAL = 10} &mdash; the exact failure mode {@link AtmosphereTuning}'s own
 * javadoc warns about for duration formatting, just not caught there. A tank reading orange while
 * the HUD still reads blue is a bug nobody would think to blame on two copies of the same two
 * numbers.
 *
 * <p>Colour choice deliberately stays with each caller: the HUD draws on an opaque background and
 * needs an alpha byte, the item bar does not, and a shared colour table would have to paper over
 * that difference for no real gain. What must not fork is the threshold.
 */
public enum OxygenLevel {
    OK,
    LOW,
    CRITICAL;

    private static final int SECONDS_LOW = 30;
    private static final int SECONDS_CRITICAL = 10;

    /** {@link AtmosphereTuning#secondsRemaining}'s {@code Integer.MAX_VALUE} reads as {@link #OK}. */
    public static OxygenLevel forSecondsRemaining(int seconds) {
        if (seconds <= SECONDS_CRITICAL) {
            return CRITICAL;
        }
        return seconds <= SECONDS_LOW ? LOW : OK;
    }
}
