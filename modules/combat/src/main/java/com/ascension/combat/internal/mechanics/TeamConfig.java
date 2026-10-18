package com.ascension.combat.internal.mechanics;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side team settings (docs/progression/teams.md section 5, ADR-0023). Its own file, beside
 * the rune settings, so a server owner finds the team rules in one place.
 */
public final class TeamConfig {

    public static final ModConfigSpec SPEC;
    public static final TeamConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new TeamConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.BooleanValue friendlyFire;

    private TeamConfig(ModConfigSpec.Builder builder) {
        builder.comment("Teams").push("teams");

        friendlyFire = builder
                .comment(
                        "Whether players on the same team can hurt each other.",
                        "",
                        "false (the default): damage between teammates, and from a teammate's arrows, pets",
                        "and area effects, is cancelled, so a big hit in a crowded fight cannot kill a friend",
                        "by accident. Only real teams are protected: with no team mod installed everyone is",
                        "on one stand-in team, which this never covers, so pvp stays whatever the server's",
                        "pvp setting says. Fighting another team is up to that setting too.",
                        "true: no protection; the server's pvp setting alone decides.")
                .define("friendlyFire", false);

        builder.pop();
    }
}
