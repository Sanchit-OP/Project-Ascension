package com.ascension.compat.chunky.internal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration.
 *
 * <p>One switch, and it is off. On 2026-10-10 the background pre-generation crashed a fresh
 * dedicated server with nobody online: generated chunks were not being unloaded while Chunky
 * worked, so they piled up until the 6 GB heap was full (the bug in
 * {@code plans/m3-vertical-slice.md}). Until that is found, a server only pre-generates when its
 * admin turns this on, or runs {@code /chunky} by hand.
 */
public final class ChunkyCompatConfig {

    public static final ModConfigSpec SPEC;
    public static final ChunkyCompatConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ChunkyCompatConfig(builder);
        SPEC = builder.build();
    }

    /** Whether every planet's surface is pre-generated in the background from server start. */
    public final ModConfigSpec.BooleanValue background;

    private ChunkyCompatConfig(ModConfigSpec.Builder builder) {
        builder.comment("Pre-generating the planets with Chunky").push("pregeneration");

        background = builder
                .comment(
                        "Pre-generate every planet's surface in the background, one planet at a time,",
                        "from the moment the server starts, out to 100 chunks from each planet's centre.",
                        "",
                        "Off by default: with some mod sets, chunks generated this way are not unloaded",
                        "while it runs, and memory fills until the server crashes. Turn it on only on a",
                        "server with memory to spare, and watch it. /chunky still works by hand either way.")
                .define("background", false);

        builder.pop();
    }

    public boolean backgroundEnabled() {
        return background.getAsBoolean();
    }
}
