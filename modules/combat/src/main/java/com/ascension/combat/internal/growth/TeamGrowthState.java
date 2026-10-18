package com.ascension.combat.internal.growth;

import com.ascension.core.api.TeamRegistry;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * Each team's shared milestones earned so far: the advancement ids of every {@code shared}
 * {@link GrowthMilestone} a member of that team has earned (docs/progression/teams.md, ADR-0023).
 * A player joining a team is awarded each of them, which is how a late joiner arrives with the
 * team's points.
 *
 * <p>A team is whatever {@link TeamRegistry} says: a party, a player alone, or {@link
 * TeamRegistry#EVERYONE} when no team mod is installed. Each team has its own set, and <strong>a
 * team holds the union of its members' milestones</strong> ({@link #join}).
 *
 * <p>Lives on the Overworld, the same "attach to the one level every server has" choice {@code
 * ascension-worlds} makes for its gateway network. Level attachments are saved with the level and
 * freed with it (ADR-0007 rule 3).
 *
 * <p><strong>Worlds saved before teams</strong> hold one flat list, the whole server's. It reads as
 * {@link TeamRegistry#EVERYONE}'s set, and every team seen for the first time starts with a copy of
 * it, so turning parties on in an existing world takes nothing from anyone.
 */
public final class TeamGrowthState {

    private static final Codec<List<ResourceLocation>> IDS = ResourceLocation.CODEC.listOf();

    /** The new shape: a list per team id. Tried first; the old flat list is read as a fallback. */
    public static final Codec<TeamGrowthState> CODEC = Codec.either(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, IDS), IDS)
            .xmap(either -> either.map(TeamGrowthState::fromTeams, TeamGrowthState::fromOldList),
                    state -> Either.left(state.toTeams()));

    private final Map<UUID, Set<ResourceLocation>> byTeam = new LinkedHashMap<>();

    public TeamGrowthState() {
    }

    private static TeamGrowthState fromTeams(Map<UUID, List<ResourceLocation>> teams) {
        TeamGrowthState state = new TeamGrowthState();
        teams.forEach((team, ids) -> state.byTeam.put(team, new LinkedHashSet<>(ids)));
        return state;
    }

    private static TeamGrowthState fromOldList(List<ResourceLocation> ids) {
        TeamGrowthState state = new TeamGrowthState();
        state.byTeam.put(TeamRegistry.EVERYONE, new LinkedHashSet<>(ids));
        return state;
    }

    private Map<UUID, List<ResourceLocation>> toTeams() {
        Map<UUID, List<ResourceLocation>> teams = new LinkedHashMap<>();
        byTeam.forEach((team, ids) -> teams.put(team, List.copyOf(ids)));
        return teams;
    }

    /**
     * The team's own set, made on first sight: empty for {@link TeamRegistry#EVERYONE}, a copy of
     * the whole-server set for any other team (see the class comment).
     */
    private Set<ResourceLocation> setFor(UUID team) {
        return byTeam.computeIfAbsent(team, id -> id.equals(TeamRegistry.EVERYONE)
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(byTeam.getOrDefault(TeamRegistry.EVERYONE, Set.of())));
    }

    /** The milestones this team has earned. */
    public Set<ResourceLocation> earned(UUID team) {
        return Set.copyOf(setFor(team));
    }

    /** Records a shared milestone's advancement for a team. True if the team did not have it yet. */
    boolean add(UUID team, ResourceLocation advancement) {
        return setFor(team).add(advancement);
    }

    /** Forgets one for a team, for the debug command. True if the team had it. */
    boolean remove(UUID team, ResourceLocation advancement) {
        return setFor(team).remove(advancement);
    }

    /**
     * What a player joining a team changes: the team's milestones and the player's, made one.
     *
     * @param teamHas what the team has earned
     * @param playerHolds the shared milestones the player already has
     * @param forPlayer what the player is missing, to be awarded to them
     * @param forTeam what the team is missing, to be recorded and awarded to its other members
     */
    record Join(Set<ResourceLocation> forPlayer, Set<ResourceLocation> forTeam) {
    }

    /** Both halves of the union rule (teams.md section 4): you get theirs, they get yours. */
    static Join join(Set<ResourceLocation> teamHas, Set<ResourceLocation> playerHolds) {
        Set<ResourceLocation> forPlayer = new LinkedHashSet<>(teamHas);
        forPlayer.removeAll(playerHolds);
        Set<ResourceLocation> forTeam = new LinkedHashSet<>(playerHolds);
        forTeam.removeAll(teamHas);
        return new Join(Set.copyOf(forPlayer), Set.copyOf(forTeam));
    }
}
