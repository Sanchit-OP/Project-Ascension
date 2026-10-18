package com.ascension.core.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Something that knows which team a player is on.
 *
 * <p>Register one to make Ascension's team-shaped rules (a boss's points, which players are allies,
 * who can hurt whom) follow your own idea of a team, without anything having to know your mod
 * exists. {@code ascension-compat-ftbteams} registers one backed by FTB Teams' parties.
 *
 * <p><strong>A solo player is a team of one.</strong> Return a team id for every player you know,
 * including one who has never joined anything: that id is the player's own team. Return
 * {@link Optional#empty()} only for a player you have never heard of, and the registry will treat
 * that player as a team of one too.
 *
 * <p>Called on the server thread, often (every time a boss dies, a player logs in, an ally is
 * looked for). Keep it a map read: no I/O, no allocation worth the name, and answer for an offline
 * player as well as an online one.
 */
@FunctionalInterface
public interface TeamSource {

    /**
     * The team this player is on now.
     *
     * @param player the player's profile id, online or not
     * @return a stable id for that team, or empty if this source has never heard of the player
     */
    Optional<UUID> teamOf(UUID player);
}
