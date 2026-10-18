package com.ascension.hud.internal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client configuration.
 *
 * <p>The master switch is how a pack that wants vanilla's HUD back gets it (ADR-0022): off, this
 * mod draws nothing and cancels nothing. Per-element visibility, position and scale come with the
 * elements themselves.
 */
public final class HudConfig {

    public static final ModConfigSpec SPEC;
    public static final HudConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new HudConfig(builder);
        SPEC = builder.build();
    }

    /** Draw the Ascension HUD at all. Off means vanilla's HUD and nothing of ours. */
    public final ModConfigSpec.BooleanValue enabled;

    /** Overall opacity of every element, 0.2 to 1. */
    public final ModConfigSpec.DoubleValue opacity;

    private HudConfig(ModConfigSpec.Builder builder) {
        builder.comment("Ascension HUD").push("hud");

        enabled = builder
                .comment("Draw the Ascension HUD. Turn off to get vanilla's HUD back.")
                .define("enabled", true);

        opacity = builder
                .comment("Overall opacity of every HUD element.")
                .defineInRange("opacity", 1.0, 0.2, 1.0);

        builder.pop();
    }
}
