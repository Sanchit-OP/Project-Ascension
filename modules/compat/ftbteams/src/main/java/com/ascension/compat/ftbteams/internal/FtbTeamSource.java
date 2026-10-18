package com.ascension.compat.ftbteams.internal;

import com.ascension.core.api.TeamSource;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.Optional;
import java.util.UUID;

/**
 * Which team a player is on, according to FTB Teams.
 *
 * <p>FTB Teams gives every player a personal team and puts them in a party's team while they are in
 * one, so the answer is the party's id or, for a solo player, their own: a team of one, exactly as
 * {@link TeamSource} asks. {@code getTeamId} is used, not {@code getId}, because on a personal team it
 * is the id of the team the player is <em>currently</em> in.
 *
 * <p>A lookup in FTB Teams' own map, and it works for a player who is offline. Before FTB Teams has
 * loaded its data (the server is still starting) it says it has never heard of anyone, which
 * {@code TeamRegistry} reads as a team of one.
 */
public final class FtbTeamSource implements TeamSource {

    @Override
    public Optional<UUID> teamOf(UUID player) {
        FTBTeamsAPI.API api = FTBTeamsAPI.api();
        if (!api.isManagerLoaded()) {
            return Optional.empty();
        }

        return api.getManager().getTeamForPlayerID(player).map(Team::getTeamId);
    }
}
