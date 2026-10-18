package com.ascension.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.TeamRegistry;
import com.ascension.core.api.TeamSource;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the shared team holder.
 *
 * <p>Per ADR-0008 this is the kind of thing a unit test may decide: ids in, ids out, no player or
 * world involved. Whether a real server really hands a boss's points to the right team is not
 * decided here, it is seen in a game.
 */
class TeamHolderTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID PARTY = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    /** Alice and Bob share a party; Carol is not in the source at all. */
    private static final TeamSource PARTY_OF_TWO = player -> Optional.ofNullable(Map.of(ALICE, PARTY, BOB, PARTY).get(player));

    @Test
    void everyoneIsOneTeamWhenNothingIsRegistered() {
        TeamHolder holder = new TeamHolder();

        assertEquals(TeamRegistry.EVERYONE, holder.teamOf(ALICE));
        assertEquals(TeamRegistry.EVERYONE, holder.teamOf(BOB));
        assertTrue(holder.sameTeam(ALICE, BOB));
    }

    @Test
    void theDefaultTeamIsNeverARealPlayersId() {
        assertNotEquals(TeamRegistry.EVERYONE, ALICE);
        assertNotEquals(TeamRegistry.EVERYONE, PARTY);
    }

    @Test
    void answersAccordingToTheRegisteredSource() {
        TeamHolder holder = new TeamHolder();
        holder.register(PARTY_OF_TWO);

        assertEquals(PARTY, holder.teamOf(ALICE));
        assertEquals(PARTY, holder.teamOf(BOB));
        assertTrue(holder.sameTeam(ALICE, BOB));
    }

    @Test
    void aPlayerTheSourceDoesNotKnowIsATeamOfOne() {
        TeamHolder holder = new TeamHolder();
        holder.register(PARTY_OF_TWO);

        assertEquals(CAROL, holder.teamOf(CAROL));
        assertFalse(holder.sameTeam(CAROL, ALICE));
        assertFalse(holder.sameTeam(ALICE, CAROL));
    }

    @Test
    void aPlayerIsAlwaysOnTheirOwnTeam() {
        TeamHolder holder = new TeamHolder();
        holder.register(PARTY_OF_TWO);

        assertTrue(holder.sameTeam(CAROL, CAROL));
    }

    @Test
    void registeringASecondSourceThrows() {
        TeamHolder holder = new TeamHolder();
        holder.register(PARTY_OF_TWO);

        assertThrows(IllegalStateException.class, () -> holder.register(PARTY_OF_TWO));
    }
}
