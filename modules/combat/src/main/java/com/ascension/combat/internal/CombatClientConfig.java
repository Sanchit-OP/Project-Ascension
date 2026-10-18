package com.ascension.combat.internal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * This module's client-side settings, kept in {@code ascension_combat-client.toml}. Nothing here changes
 * what the game does, only what one player sees.
 */
public final class CombatClientConfig {

    public static final ModConfigSpec SPEC;
    public static final CombatClientConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new CombatClientConfig(builder);
        SPEC = builder.build();
    }

    /** How strongly the magic-hit screen tint shows, 0 (off) to 1. */
    public final ModConfigSpec.DoubleValue magicHitTint;

    private CombatClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("What you see").push("display");

        magicHitTint = builder
                .comment(
                        "The faint blue tint at the edge of the screen when a magic hit lands on you",
                        "(blasts, fire, lightning, potions, the Warden's sonic boom...; see damage-kinds.md).",
                        "1 is the normal strength, which is already small. 0 turns it off.")
                .defineInRange("magicHitTint", 1.0, 0.0, 1.0);

        builder.pop();
    }
}
