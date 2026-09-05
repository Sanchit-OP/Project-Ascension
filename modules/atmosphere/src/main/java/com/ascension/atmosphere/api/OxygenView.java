package com.ascension.atmosphere.api;

/**
 * Read-only view of a player's oxygen situation.
 *
 * <p>Handed out to anything that wants to display or react to oxygen without being able to
 * change it. Mutation goes through the module's own systems so that accounting, syncing and
 * the failure window stay consistent.
 */
public interface OxygenView {

    /** Units currently held across all of the player's sources. */
    int available();

    /** Total units the player could hold if everything were full. */
    int capacity();

    /** The atmosphere at the player's current position. */
    Atmosphere atmosphere();

    /**
     * Remaining air in seconds at the current drain rate, or {@link Integer#MAX_VALUE} when in
     * breathable air.
     *
     * <p>This is the figure meant for players. "40 seconds" is something you can act on;
     * "1200 units" is not.
     */
    int secondsRemaining();

    /** Whether the player is inside the failure window with no supply left. */
    boolean suffocating();
}
