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

    /** Whether only a vehicle can carry a player into space, once one exists (ADR-0015). */
    public final ModConfigSpec.BooleanValue vehicleOnly;

    private WorldsConfig(ModConfigSpec.Builder builder) {
        builder.comment("Reaching space").push("ascent");

        vehicleOnly = builder
                .comment(
                        "Only a vehicle can carry a player into space.",
                        "",
                        "With this on, and a vehicle bridge installed (ascension-compat-sable, for Create",
                        "Aeronautics ships), rising past a planet's build height on foot does nothing: no",
                        "towers, no elytra, no fireworks. The player is told that only a ship can carry",
                        "them further, and has to build one.",
                        "",
                        "Without a vehicle bridge installed this does nothing, so space is never left",
                        "unreachable. Creative and spectator players are never stopped.")
                .define("vehicleOnly", true);

        builder.pop();

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
