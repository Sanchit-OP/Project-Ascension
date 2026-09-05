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
            new OxygenSyncPayload(0, AtmosphereTuning.TANK_CAPACITY, true, 0.0f, false);

    private ClientOxygenState() {
    }

    public static void accept(OxygenSyncPayload payload) {
        current = payload;
    }

    /** Reset on disconnect, so a stale bar cannot survive into the next session. */
    public static void clear() {
        current = new OxygenSyncPayload(0, AtmosphereTuning.TANK_CAPACITY, true, 0.0f, false);
    }

    public static OxygenSyncPayload current() {
        return current;
    }

    /** Whether the HUD has anything worth drawing. */
    public static boolean shouldRender() {
        OxygenSyncPayload state = current;
        return !state.breathable() || state.suffocating();
    }

    public static int secondsRemaining() {
        OxygenSyncPayload state = current;
        return AtmosphereTuning.secondsRemaining(state.units(), state.drainPerSecond());
    }
}
