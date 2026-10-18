package com.ascension.core.api;

import com.ascension.core.internal.TeamHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Ask which team a player is on, or say that yours decides it.
 *
 * <p>The meeting point for a question more than one Tier 1 module needs answered (ADR-0011,
 * ADR-0023): {@code ascension-combat} asks it to hand a boss's points to the killer's team and to
 * find a player's allies; {@code ascension-compat-ftbteams} answers it from FTB Teams' parties;
 * neither needs the other, or FTB Teams, to be there.
 *
 * <p>Deliberately <strong>single-source</strong>, like {@link CombatStateRegistry}: exactly one
 * module says who is on which team. <strong>With none registered, everyone is on one team</strong>
 * ({@link #EVERYONE}), which is how the game behaved before it had teams, so a server without a
 * team mod needs nothing.
 *
 * <p>A team is only an id. This registry says who is on it, not what it is called, who owns it or
 * how to join one: that is the team mod's own screen.
 */
public final class TeamRegistry {

    /** The one team everyone is on when no source is registered. Never a real player's or party's id. */
    public static final UUID EVERYONE = new UUID(0L, 0L);

    private TeamRegistry() {
    }

    /**
     * Claim ownership of "who is on which team". Register during mod setup.
     *
     * @throws IllegalStateException if a source is already registered
     */
    public static void register(TeamSource source) {
        TeamHolder.get().register(source);
    }

    /**
     * The team this player is on: {@link #EVERYONE} if no source is registered, the player's own id
     * (a team of one) if the source has never heard of them. Works for an offline player.
     */
    public static UUID teamOf(UUID player) {
        return TeamHolder.get().teamOf(player);
    }

    public static UUID teamOf(ServerPlayer player) {
        return teamOf(player.getUUID());
    }

    /** Whether two players are on the same team. A player is on their own team. */
    public static boolean sameTeam(UUID a, UUID b) {
        return TeamHolder.get().sameTeam(a, b);
    }

    public static boolean sameTeam(ServerPlayer a, ServerPlayer b) {
        return sameTeam(a.getUUID(), b.getUUID());
    }

    /**
     * The players of a team who are online now.
     *
     * <p>Walks the online player list, so call it when something happens (a boss dies, someone logs
     * in), never every tick (ADR-0007).
     */
    public static List<ServerPlayer> onlineMembers(MinecraftServer server, UUID team) {
        List<ServerPlayer> members = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamOf(player).equals(team)) {
                members.add(player);
            }
        }
        return members;
    }

    /**
     * For the module that registered the {@link TeamSource}: tell everyone a player has changed
     * team. Call it on the server thread once {@link #teamOf(UUID)} already answers the new team.
     *
     * @param previousTeam the team the player left, or {@code null} if the source does not know
     */
    public static void announceChange(MinecraftServer server, UUID player, UUID previousTeam, UUID team) {
        NeoForge.EVENT_BUS.post(new PlayerTeamChangedEvent(server, player, previousTeam, team));
    }
}
