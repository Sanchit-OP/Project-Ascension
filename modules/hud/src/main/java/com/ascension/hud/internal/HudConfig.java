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

    /** Ids of elements to leave out, e.g. {@code ascension_combat:stat_block}. */
    public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> hiddenElements;

    /** Size of every element, as a multiple. */
    public final ModConfigSpec.DoubleValue scale;

    private HudConfig(ModConfigSpec.Builder builder) {
        builder.comment("Ascension HUD").push("hud");

        enabled = builder
                .comment("Draw the Ascension HUD. Turn off to get vanilla's HUD back.")
                .define("enabled", true);

        opacity = builder
                .comment("Overall opacity of every HUD element.")
                .defineInRange("opacity", 1.0, 0.2, 1.0);

        scale = builder
                .comment("Size of every HUD element, as a multiple of normal. The GUI scale still applies.")
                .defineInRange("scale", 1.0, 0.6, 1.6);

        hiddenElements = builder
                .comment(
                        "Elements to leave out, by id. Known ids:",
                        "  ascension_hud:vitals          level, health and hunger",
                        "  ascension_combat:stat_block   the stat block",
                        "  ascension_combat:rune_chips   the rune chips",
                        "  ascension_combat:skill_points the unspent skill points note",
                        "  ascension_atmosphere:oxygen   the oxygen emblem")
                .defineListAllowEmpty("hiddenElements", java.util.List.of(), () -> "", o -> o instanceof String);

        builder.pop();
    }
}
