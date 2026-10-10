package com.ascension.compat.chunky.internal;

/**
 * Whether the planets' background pre-generation should pause or carry on, from how hard the
 * server is working. Numbers in, a decision out, so it is unit-tested ({@code PregenGovernorTest});
 * {@link ChunkySequentialPregen} does the asking and the pausing.
 *
 * <p><strong>What it is for.</strong> A background pre-generation must never lag the players who
 * are online, so when the server's own tick time says it is falling behind, ours waits.
 *
 * <p><strong>What it is not for.</strong> It was written on 2026-10-10 against a crash it turned
 * out not to catch: on a fresh world, generated chunks stayed loaded while Chunky ran, and the
 * heap filled while ticks were still at 3 to 10 ms, then one tick took 60 seconds. That is why
 * background pre-generation is now off by default ({@link ChunkyCompatConfig}). Heap use is no
 * better a signal under generational ZGC, whose "used" figure includes garbage not yet collected
 * and swings with every cycle.
 */
final class PregenGovernor {

    /** Pause above this: 80% of the 50 ms a tick is allowed. */
    static final double PAUSE_ABOVE_MS = 40.0;

    /** Resume below this. The gap between the two stops it flapping at the edge. */
    static final double RESUME_BELOW_MS = 25.0;

    enum Action { NONE, PAUSE, RESUME }

    private PregenGovernor() {
    }

    /**
     * @param running       one of our pre-generation tasks is generating right now
     * @param pausedForLoad one of ours is paused because this said {@link Action#PAUSE} earlier;
     *                      a task paused by anyone else is never resumed here
     * @param msPerTick     the server's average tick time, in milliseconds
     */
    static Action decide(boolean running, boolean pausedForLoad, double msPerTick) {
        if (running) {
            return msPerTick > PAUSE_ABOVE_MS ? Action.PAUSE : Action.NONE;
        }
        return pausedForLoad && msPerTick < RESUME_BELOW_MS ? Action.RESUME : Action.NONE;
    }
}
