package com.ascension.compat.ftbteams.internal;

import com.ascension.core.api.TeamRegistry;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Passes FTB Teams' "this player changed team" on to Ascension.
 *
 * <p>FTB Teams fires it for a party joined, a party left, one disbanded, and for a player's first
 * login, and fires it <em>after</em> the player's team has changed, so {@code TeamRegistry.teamOf}
 * already answers the new one when Ascension's listeners run. Ascension's listeners are written to be
 * run twice for the same change, so the first-login case costs nothing.
 *
 * <p>FTB Teams' events are Architectury events, not NeoForge ones, which is the whole reason this is a
 * jar of its own.
 */
public final class FtbTeamEvents {

    private FtbTeamEvents() {
    }

    public static void register() {
        TeamEvent.PLAYER_CHANGED.register(FtbTeamEvents::onPlayerChanged);
    }

    private static void onPlayerChanged(PlayerChangedTeamEvent event) {
        MinecraftServer server = FTBTeamsAPI.api().getManager().getServer();
        UUID previous = event.getPreviousTeam().map(Team::getTeamId).orElse(null);
        TeamRegistry.announceChange(server, event.getPlayerId(), previous, event.getTeam().getTeamId());
    }
}
