package com.ascension.hud.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Measures what the HUD layer costs per frame, for {@code docs/technical/performance-log.md}
 * (ADR-0007 asks for a recorded reading because the HUD runs every frame). Off unless a dev script
 * starts it: a frame then costs two clock reads, and a normal run costs one boolean read.
 */
@OnlyIn(Dist.CLIENT)
public final class HudFrameCost {

    private static volatile boolean armed;
    private static long totalNanos;
    private static long maxNanos;
    private static int frames;

    private HudFrameCost() {
    }

    /** The time to hand back to {@link #end}, or 0 when not measuring. */
    static long begin() {
        return armed ? System.nanoTime() : 0L;
    }

    static void end(long started) {
        if (started == 0L) {
            return;
        }
        long took = System.nanoTime() - started;
        totalNanos += took;
        maxNanos = Math.max(maxNanos, took);
        frames++;
    }

    /** Starts a fresh measurement. */
    public static void start() {
        totalNanos = 0L;
        maxNanos = 0L;
        frames = 0;
        armed = true;
    }

    /** Stops and describes what was measured. */
    public static String stop() {
        armed = false;
        if (frames == 0) {
            return "no frames measured";
        }
        return String.format(java.util.Locale.ROOT, "%d frames, average %.1f us, worst %.1f us",
                frames, totalNanos / 1000.0 / frames, maxNanos / 1000.0);
    }
}
