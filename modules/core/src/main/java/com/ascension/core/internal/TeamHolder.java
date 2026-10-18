package com.ascension.core.internal;

import com.ascension.core.api.TeamRegistry;
import com.ascension.core.api.TeamSource;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the single registered team source and answers queries against it.
 *
 * <p>Single-source for the same reason {@link CombatStateHolder} is: which team a player is on is
 * not a question two modules should answer independently. One module (FTB Teams' connector) says,
 * everyone else asks.
 *
 * <p><strong>With no source registered, everyone is on one team</strong>
 * ({@link TeamRegistry#EVERYONE}). That is what makes every Tier 1 module work with only {@code
 * core} present, and it is what the game did before it had teams.
 *
 * <p>Stores ids only, never a player or a server (ADR-0007 rule 1).
 */
public final class TeamHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamHolder.class);

    private static final TeamHolder INSTANCE = new TeamHolder();

    private volatile TeamSource source;

    /** Package-private so tests can build their own instance, as {@link CombatStateHolder} does. */
    TeamHolder() {
    }

    public static TeamHolder get() {
        return INSTANCE;
    }

    /**
     * @throws IllegalStateException if a source is already registered
     */
    public void register(TeamSource newSource) {
        if (source != null) {
            throw new IllegalStateException(
                    "Team source already registered. Only one module should say who is on which team.");
        }
        source = newSource;
        LOGGER.info("Team source registered");
    }

    /** The player's team: everyone's one team if nothing registered, the player's own if the source does not know them. */
    public UUID teamOf(UUID player) {
        TeamSource snapshot = source;
        if (snapshot == null) {
            return TeamRegistry.EVERYONE;
        }
        return snapshot.teamOf(player).orElse(player);
    }

    public boolean sameTeam(UUID a, UUID b) {
        return a.equals(b) || teamOf(a).equals(teamOf(b));
    }
}
