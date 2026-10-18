package com.ascension.core.api;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * A player has moved to another team: joined a party, left one, or had their party disbanded.
 *
 * <p>Posted on the game event bus ({@code NeoForge.EVENT_BUS}), on the server thread, by whoever
 * registered the {@link TeamSource} (through {@link TeamRegistry#announceChange}). It is posted
 * <em>after</em> the change, so {@link TeamRegistry#teamOf(UUID)} already answers the new team.
 *
 * <p>The player may be offline: an operator can remove someone from a party while they are away.
 * {@link #onlinePlayer()} is empty then, and a listener that needs the player acts when they next
 * log in instead.
 *
 * <p>Short-lived by design: it holds the server for the length of the dispatch and nothing is meant
 * to keep it (ADR-0007 rule 1).
 */
public final class PlayerTeamChangedEvent extends Event {

    private final MinecraftServer server;
    private final UUID player;
    private final UUID previousTeam;
    private final UUID team;

    PlayerTeamChangedEvent(MinecraftServer server, UUID player, UUID previousTeam, UUID team) {
        this.server = server;
        this.player = player;
        this.previousTeam = previousTeam;
        this.team = team;
    }

    public MinecraftServer server() {
        return this.server;
    }

    /** The player's profile id. */
    public UUID player() {
        return this.player;
    }

    /** The player, if they are online. */
    public Optional<ServerPlayer> onlinePlayer() {
        return Optional.ofNullable(this.server.getPlayerList().getPlayer(this.player));
    }

    /** The team the player was on before, if the source said. */
    public Optional<UUID> previousTeam() {
        return Optional.ofNullable(this.previousTeam);
    }

    /** The team the player is on now. */
    public UUID team() {
        return this.team;
    }
}
