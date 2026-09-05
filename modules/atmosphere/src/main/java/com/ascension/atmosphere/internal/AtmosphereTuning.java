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
     * Where portable tanks sit in the consumption order. Lower is drained first.
     *
     * <p>Deliberately low, with plenty of room above it: suit-integrated reserve, vehicle supply
     * and anything else that ought to be a safety margin gets a higher number, so running a tank
     * dry is a warning rather than the moment you start dying.
     */
    public static final int TANK_DRAW_ORDER = 100;

    /**
     * Everyone's built-in reserve: what fits in a pair of lungs.
     *
     * <p>Twenty seconds at baseline drain. Refills for free in breathable air, exactly like
     * vanilla air bubbles, and is drawn from <em>last</em> so it is the buffer that gets you
     * back to air after a tank runs dry.
     */
    public static final int LUNG_CAPACITY = BASE_UNITS_PER_SECOND * 20;

    /**
     * How fast lungs refill in breathable air.
     *
     * <p>Roughly four seconds from empty, close to vanilla's bubble refill. Fast enough not to
     * be a punishment, slow enough that surfacing for air reads as an action rather than a
     * formality.
     */
    public static final int LUNG_REFILL_PER_SECOND = 20;

    /**
     * How long a freshly opened tank takes before it delivers any air.
     *
     * <p>This is the whole anti-stockpile mechanism, and it is deliberately the only one that
     * cannot be dodged. Capping the inventory stops a player carrying ten tanks; it does nothing
     * about ten tanks in a shulker box, and chasing every container mod is a fight we would lose.
     * A swap that costs real time inside the failure window does not care where the spares were
     * hidden.
     *
     * <p>Three seconds against a twenty-second lung reserve: enough that swapping mid-crisis is a
     * decision, not enough that it is a death sentence.
     */
    public static final int TANK_PRESSURISE_TICKS = 60;

    /**
     * Tanks a player may carry in their own inventory.
     *
     * <p>Two: the one on the valve and one spare. Enough to plan a longer trip, not enough to
     * replace a refill station with a bag of tanks.
     *
     * <p>Counts the player's own 41 slots only. Anything inside another container &mdash; a
     * shulker box, a backpack &mdash; is invisible here, by acknowledged design rather than
     * oversight. {@link #TANK_PRESSURISE_TICKS} is what covers that gap.
     */
    public static final int MAX_TANKS_CARRIED = 2;

    /** How often the carry rules are swept. Only runs at all while a player holds a tank. */
    public static final int TANK_SWEEP_INTERVAL_TICKS = 20;

    /**
     * Ticks between running out of air and taking the first damage.
     *
     * <p>The "short failure window" from {@code docs/gameplay/oxygen.md}: long enough to turn
     * and run for a door, short enough that ignoring the bar kills you.
     */
    public static final int SUFFOCATION_GRACE_TICKS = 40;

    /** Damage applied per second once the grace window has elapsed. Matches vanilla drowning. */
    public static final float SUFFOCATION_DAMAGE = 2.0f;

    /**
     * Largest room a single emitter can pressurise, in blocks.
     *
     * <p>A hard cap, not a guideline. Without one, an emitter placed outdoors would flood-fill
     * every loaded chunk, and this runs on block changes.
     */
    public static final int SEALED_VOLUME_LIMIT = 4096;

    /** How far from the emitter the fill may reach, in blocks along any axis. */
    public static final int SEALED_VOLUME_RADIUS = 24;

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

    /**
     * Seconds as something a player can read.
     *
     * <p>Lives here rather than in the HUD because the HUD is client-only and tank tooltips,
     * station messages and commands all need the same string. Two formatters would eventually
     * disagree, and the first place anyone would notice is a tooltip claiming a different
     * number from the bar.
     *
     * @return {@code "--"} for {@link Integer#MAX_VALUE}, meaning nothing is draining
     */
    public static String formatDuration(int seconds) {
        if (seconds == Integer.MAX_VALUE) {
            return "--";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}
