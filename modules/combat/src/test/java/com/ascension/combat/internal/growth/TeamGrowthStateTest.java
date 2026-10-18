package com.ascension.combat.internal.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.TeamRegistry;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Each team's milestones, how they are saved (including a world saved before teams existed), and the
 * union rule for joining a team (docs/progression/teams.md sections 3 and 4). That a boss's points
 * really reach the right players is for a running server (ADR-0008); this is the arithmetic and the
 * file format under it.
 */
class TeamGrowthStateTest {

    private static final UUID PARTY = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    private static final ResourceLocation WARDEN = id("growth/warden");
    private static final ResourceLocation WITHER = id("growth/wither");
    private static final ResourceLocation MOON_GATE = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "gates/moon");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ascension_combat", path);
    }

    private static TeamGrowthState saveAndLoad(TeamGrowthState state) {
        Tag saved = TeamGrowthState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        return TeamGrowthState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    }

    @Test
    void aTeamEarnsEachMilestoneOnce() {
        TeamGrowthState state = new TeamGrowthState();

        assertTrue(state.add(PARTY, WARDEN), "the first time it is new");
        assertFalse(state.add(PARTY, WARDEN), "the second time it is not");
        assertEquals(Set.of(WARDEN), state.earned(PARTY));
    }

    @Test
    void teamsKeepTheirOwnMilestones() {
        TeamGrowthState state = new TeamGrowthState();
        state.add(PARTY, WARDEN);

        assertEquals(Set.of(), state.earned(OTHER), "another party's kill does not pay this one");
        assertTrue(state.add(OTHER, WARDEN), "so the other party can still earn it");
    }

    @Test
    void forgettingOnlyAffectsThatTeam() {
        TeamGrowthState state = new TeamGrowthState();
        state.add(PARTY, WARDEN);
        state.add(OTHER, WARDEN);

        assertTrue(state.remove(PARTY, WARDEN));
        assertEquals(Set.of(), state.earned(PARTY));
        assertEquals(Set.of(WARDEN), state.earned(OTHER));
        assertFalse(state.remove(PARTY, WARDEN));
    }

    @Test
    void everyTeamIsSavedAndLoadedAgain() {
        TeamGrowthState state = new TeamGrowthState();
        state.add(PARTY, WARDEN);
        state.add(PARTY, MOON_GATE);
        state.add(OTHER, WITHER);

        TeamGrowthState loaded = saveAndLoad(state);

        assertEquals(Set.of(WARDEN, MOON_GATE), loaded.earned(PARTY));
        assertEquals(Set.of(WITHER), loaded.earned(OTHER));
    }

    @Test
    void aWorldSavedBeforeTeamsReadsAsTheWholeServersSet() {
        // What the game wrote when the team was the whole server: one flat list of advancement ids.
        Tag old = ResourceLocation.CODEC.listOf().encodeStart(NbtOps.INSTANCE, List.of(WARDEN, MOON_GATE)).getOrThrow();

        TeamGrowthState loaded = TeamGrowthState.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();

        assertEquals(Set.of(WARDEN, MOON_GATE), loaded.earned(TeamRegistry.EVERYONE));
    }

    @Test
    void aTeamSeenForTheFirstTimeStartsWithWhatTheWholeServerHad() {
        // Turning parties on in a world that has been played: nobody loses a point they had.
        Tag old = ResourceLocation.CODEC.listOf().encodeStart(NbtOps.INSTANCE, List.of(WARDEN)).getOrThrow();
        TeamGrowthState state = TeamGrowthState.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();

        assertEquals(Set.of(WARDEN), state.earned(PARTY));
        assertTrue(state.add(PARTY, WITHER));
        assertEquals(Set.of(WARDEN), state.earned(OTHER), "the copy is made once, then the teams part");
        assertEquals(Set.of(WARDEN), state.earned(TeamRegistry.EVERYONE), "and the whole server's set is untouched");
    }

    @Test
    void aFreshWorldsTeamsStartEmpty() {
        TeamGrowthState state = new TeamGrowthState();

        assertEquals(Set.of(), state.earned(PARTY));
        assertEquals(Set.of(), state.earned(TeamRegistry.EVERYONE));
    }

    @Test
    void joiningGivesYouTheirsAndThemYours() {
        TeamGrowthState.Join join = TeamGrowthState.join(Set.of(WARDEN, MOON_GATE), Set.of(WITHER, MOON_GATE));

        assertEquals(Set.of(WARDEN), join.forPlayer(), "the player gets the party's Warden, not the gate they hold");
        assertEquals(Set.of(WITHER), join.forTeam(), "the party gets the player's Wither, not the gate it holds");
    }

    @Test
    void joiningATeamThatHasNothingTakesNothing() {
        TeamGrowthState.Join join = TeamGrowthState.join(Set.of(), Set.of(WARDEN));

        assertEquals(Set.of(), join.forPlayer());
        assertEquals(Set.of(WARDEN), join.forTeam());
    }

    @Test
    void joiningWithNothingToOfferOnlyReceives() {
        TeamGrowthState.Join join = TeamGrowthState.join(Set.of(WARDEN), Set.of());

        assertEquals(Set.of(WARDEN), join.forPlayer());
        assertEquals(Set.of(), join.forTeam());
    }

    @Test
    void twoPlayersWithTheSameMilestonesChangeNothing() {
        TeamGrowthState.Join join = TeamGrowthState.join(Set.of(WARDEN), Set.of(WARDEN));

        assertEquals(Set.of(), join.forPlayer());
        assertEquals(Set.of(), join.forTeam());
    }
}
