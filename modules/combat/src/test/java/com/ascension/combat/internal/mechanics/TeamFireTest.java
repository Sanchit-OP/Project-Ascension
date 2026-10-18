package com.ascension.combat.internal.mechanics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.TeamRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Which pairs of teams friendly fire protects. That a hit is really cancelled, and starts nothing else,
 * is for a running game with two players (ADR-0008); this is only the rule's edges.
 */
class TeamFireTest {

    private static final UUID PARTY = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    @Test
    void membersOfOneRealTeamAreProtected() {
        assertTrue(TeamFire.protects(PARTY, PARTY));
    }

    @Test
    void differentTeamsAreNot() {
        assertFalse(TeamFire.protects(PARTY, OTHER));
    }

    @Test
    void theStandInTeamIsNeverProtected() {
        // With no team mod everyone is one team, and that is not a group of friends.
        assertFalse(TeamFire.protects(TeamRegistry.EVERYONE, TeamRegistry.EVERYONE));
    }
}
