package com.ascension.combat.internal.rune;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side rune settings (docs/technical/rune-system.md sections 6 and 7). Defaults are
 * Project-Ascension's; another pack can make runes free or open from the start in one line.
 */
public final class RuneConfig {

    public enum Unlocks {
        /** Every slot open from the first minute. */
        ALL,
        /** The secondary path and the shard rows open with growth's milestones. */
        PROGRESSIVE
    }

    public static final ModConfigSpec SPEC;
    public static final RuneConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new RuneConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.EnumValue<Unlocks> unlocks;
    public final ModConfigSpec.IntValue changeChalk;
    public final ModConfigSpec.IntValue changeLevels;
    public final ModConfigSpec.BooleanValue outOfCombatOnly;

    private RuneConfig(ModConfigSpec.Builder builder) {
        builder.comment("Rune pages").push("runes");

        unlocks = builder
                .comment(
                        "Which parts of the rune page are open.",
                        "",
                        "ALL: the whole page from the first minute.",
                        "PROGRESSIVE: the primary path from the start; the secondary path at a player's",
                        "first milestone, then one shard row at each of the second, third and fourth.",
                        "Milestones come from player growth; until that is installed, PROGRESSIVE opens",
                        "only the primary path.")
                .defineEnum("unlocks", Unlocks.ALL);

        changeChalk = builder
                .comment(
                        "Rune Chalk used by a save that removes or replaces a rune. Filling an empty",
                        "slot is always free. 0 makes changes need no chalk.")
                .defineInRange("changeChalk", 1, 0, 64);

        changeLevels = builder
                .comment(
                        "Experience levels spent by a save that removes or replaces a rune, alongside",
                        "the chalk. 0 makes changes cost no levels.")
                .defineInRange("changeLevels", 3, 0, 100);

        outOfCombatOnly = builder
                .comment("Refuse to save a rune page while its player is in combat.")
                .define("outOfCombatOnly", true);

        builder.pop();
    }
}
