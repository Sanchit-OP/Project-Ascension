package com.ascension.combat.internal.rune;

import com.ascension.combat.api.growth.GrowthProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * Which of a player's page slots are open (docs/technical/rune-system.md section 6).
 *
 * <p>Read live from growth's milestone count each time, never stored, so a milestone earned (or
 * revoked by an admin) changes the answer with nothing to keep in step. The one thing growth must
 * do is tell the player's client, which draws the padlocks: {@link #milestonesChanged}.
 */
public final class RuneUnlocks {

    private RuneUnlocks() {
    }

    public static RunePageRules.OpenSlots open(ServerPlayer player) {
        if (RuneConfig.INSTANCE.unlocks.get() == RuneConfig.Unlocks.ALL) {
            return RunePageRules.OpenSlots.ALL;
        }
        return RunePageRules.OpenSlots.atMilestones(GrowthProgress.milestonesReached(player));
    }

    /** Growth's milestones may have changed: the client is sent the page with its slots as they are now. */
    public static void milestonesChanged(ServerPlayer player) {
        Runes.sync(player);
    }
}
