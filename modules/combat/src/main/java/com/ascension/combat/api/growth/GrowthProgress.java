package com.ascension.combat.api.growth;

import com.ascension.combat.internal.growth.PlayerGrowth;
import net.minecraft.server.level.ServerPlayer;

/**
 * How far a player has come, for anything that unlocks with it (docs/progression/growth.md
 * section 11). Read-only: growth is earned through advancements, never set from outside.
 *
 * <p>The rune page reads {@link #milestonesReached} to open its secondary path and shard rows. A
 * milestone here is one of the team's: a boss's first kill or a lit gate. The smaller discoveries
 * give skill points too, but they are not counted, so wandering into a few places never opens
 * the page.
 */
public final class GrowthProgress {

    private GrowthProgress() {
    }

    /** How many boss and gate milestones this player has, their team's included. */
    public static int milestonesReached(ServerPlayer player) {
        return PlayerGrowth.sharedMilestonesReached(player);
    }
}
