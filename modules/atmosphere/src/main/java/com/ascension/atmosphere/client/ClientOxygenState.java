package com.ascension.atmosphere.client;

import com.ascension.atmosphere.internal.AtmosphereTuning;
import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The client's mirror of its own player's oxygen.
 *
 * <p>A cache of what the server last said, never a second copy of the logic. The client never
 * computes breathability or drain; if this and the server disagree, the server is right and the
 * next reconcile fixes it.
 *
 * <p>Client-only by construction: this class lives in the {@code client} package and is
 * referenced only from client-side event handlers, so it is never loaded on a dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientOxygenState {

    private static volatile OxygenSyncPayload current =
            new OxygenSyncPayload(AtmosphereTuning.LUNG_CAPACITY, AtmosphereTuning.LUNG_CAPACITY,
                    true, 0.0f, false, false, 0);

    private ClientOxygenState() {
    }

    public static void accept(OxygenSyncPayload payload) {
        current = payload;
    }

    /** Reset on disconnect, so a stale bar cannot survive into the next session. */
    public static void clear() {
        current = new OxygenSyncPayload(AtmosphereTuning.LUNG_CAPACITY, AtmosphereTuning.LUNG_CAPACITY,
                    true, 0.0f, false, false, 0);
    }

    public static OxygenSyncPayload current() {
        return current;
    }

    /**
     * Whether the HUD has anything worth drawing.
     *
     * <p>Includes the refill window, so surfacing shows the bar climbing back rather than the
     * meter simply vanishing. It hides again once lungs are full &mdash; an empty <em>tank</em>
     * does not keep the bar on screen forever, because a tank is refilled at a station and its
     * state belongs on the item.
     */
    public static boolean shouldRender() {
        OxygenSyncPayload state = current;
        return !state.breathable() || state.suffocating() || state.refilling()
                || state.pressurisingSeconds() > 0;
    }

    /**
     * Seconds of air remaining.
     *
     * <p>In breathable air nothing is draining, so the live rate is zero and would read as
     * infinity. Falling back to the baseline rate answers the question the player is actually
     * asking while watching the bar refill: how long would this last if I went back under.
     */
    public static int secondsRemaining() {
        OxygenSyncPayload state = current;
        float rate = state.drainPerSecond() > 0.0f
                ? state.drainPerSecond()
                : AtmosphereTuning.BASE_UNITS_PER_SECOND;
        return AtmosphereTuning.secondsRemaining(state.units(), rate);
    }
}
