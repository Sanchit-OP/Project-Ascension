package com.ascension.atmosphere.internal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration.
 *
 * <p>Only one switch so far, and it exists for a specific reason: taking over water means
 * suppressing a core vanilla mechanic. This module is meant to be adoptable by people who are
 * not building Project-Ascension, and silently seizing drowning would make it untrustworthy.
 * Default on for us, one line to turn off for anyone else.
 */
public final class AtmosphereConfig {

    public static final ModConfigSpec SPEC;
    public static final AtmosphereConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new AtmosphereConfig(builder);
        SPEC = builder.build();
    }

    /** Whether water counts as unbreathable atmosphere and vanilla drowning is taken over. */
    public final ModConfigSpec.BooleanValue vanillaWaterIntegration;

    private AtmosphereConfig(ModConfigSpec.Builder builder) {
        builder.comment("Integration with vanilla breathing mechanics").push("vanilla");

        vanillaWaterIntegration = builder
                .comment(
                        "Treat water as an unbreathable atmosphere handled by this mod.",
                        "",
                        "Drowning and suffocating in vacuum are the same problem: no air. With this on,",
                        "one bar and one failure timer covers both, an oxygen tank works underwater, and",
                        "Respiration, turtle helmets and conduit power feed into the same model.",
                        "",
                        "This suppresses vanilla's air supply and drowning damage while active. Turn it",
                        "off to leave vanilla breathing completely untouched.")
                .define("waterIntegration", true);

        builder.pop();
    }

    public boolean waterIntegrationEnabled() {
        // Config can be queried before load on a fresh install; default to the safe answer.
        return vanillaWaterIntegration.getAsBoolean();
    }
}
