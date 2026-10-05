package com.ascension.worlds.internal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration.
 *
 * <p>Same reasoning as {@code AtmosphereConfig}: this module is meant to be adoptable by people who
 * are not building Project-Ascension, and silently switching off a core vanilla mechanic would make
 * it untrustworthy. Default on for us, one line to turn off for anyone else.
 */
public final class WorldsConfig {

    public static final ModConfigSpec SPEC;
    public static final WorldsConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new WorldsConfig(builder);
        SPEC = builder.build();
    }

    /** Whether vanilla's Nether and End portals are dormant. */
    public final ModConfigSpec.BooleanValue disableVanillaPortals;

    private WorldsConfig(ModConfigSpec.Builder builder) {
        builder.comment("Integration with vanilla travel").push("vanilla");

        disableVanillaPortals = builder
                .comment(
                        "Make vanilla's Nether and End portals dormant.",
                        "",
                        "In Project-Ascension the Nether and the End are meant to be reached as planets,",
                        "not through vanilla portals. With this on, a Nether portal frame will not light,",
                        "and no portal of any kind will carry an entity into the Nether or the End.",
                        "Operator commands (/execute in, /tp) still work.",
                        "",
                        "Turn it off to leave vanilla portals completely untouched.")
                .define("disableVanillaPortals", true);

        builder.pop();
    }
}
