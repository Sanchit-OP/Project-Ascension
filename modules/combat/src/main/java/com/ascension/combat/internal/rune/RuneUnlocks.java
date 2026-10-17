package com.ascension.combat.internal.rune;

import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;

/**
 * Which of a player's page slots are open (docs/technical/rune-system.md section 6).
 *
 * <p>Milestones belong to player growth, a separate session's work. Until it is merged, nothing
 * counts them, so {@link #setMilestones} is the one seam it plugs into: growth hands over a
 * function from a player to their milestone count, and this reads it. The agreement goes through
 * Sanchit; nothing here reaches into growth's code.
 */
public final class RuneUnlocks {

    private static ToIntFunction<ServerPlayer> milestones = player -> 0;

    private RuneUnlocks() {
    }

    /** Growth's hook: how many milestones a player has reached. */
    public static void setMilestones(ToIntFunction<ServerPlayer> source) {
        milestones = source;
    }

    public static RunePageRules.OpenSlots open(ServerPlayer player) {
        if (RuneConfig.INSTANCE.unlocks.get() == RuneConfig.Unlocks.ALL) {
            return RunePageRules.OpenSlots.ALL;
        }
        return RunePageRules.OpenSlots.atMilestones(milestones.applyAsInt(player));
    }
}
