package com.ascension.compat.chunky.internal;

import com.mojang.serialization.Codec;

/**
 * Marks whether a pre-generation task has already been started for the level this is attached
 * to. Deliberately coarse &mdash; "started", not "finished successfully" or "covers exactly this
 * radius" &mdash; because the only thing this exists to prevent is starting a second task for a
 * level that already has one going or already had one run, not to track its progress.
 */
public final class PregenMarker {

    public static final Codec<PregenMarker> CODEC =
            Codec.BOOL.xmap(PregenMarker::fromStarted, PregenMarker::started);

    private boolean started;

    boolean started() {
        return started;
    }

    void markStarted() {
        this.started = true;
    }

    private static PregenMarker fromStarted(boolean started) {
        PregenMarker marker = new PregenMarker();
        marker.started = started;
        return marker;
    }
}
