package com.ascension.combat.internal.mechanics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** How much shorter an ability's cooldown is with haste (docs/technical/rune-system.md section 4.3). Pure numbers (ADR-0008). */
class ItemAbilitiesTest {

    @Test
    void noHasteLeavesTheCooldownAlone() {
        assertEquals(240, ItemAbilities.scaled(240, 0.0));
    }

    @Test
    void twentyPercentFasterDividesByOnePointTwo() {
        assertEquals(200, ItemAbilities.scaled(240, 0.20));
        assertEquals(100, ItemAbilities.scaled(120, 0.20));
    }

    @Test
    void hasteAddsUp() {
        assertEquals(160, ItemAbilities.scaled(240, 0.50));
    }

    @Test
    void aCooldownIsNeverShorterThanOneTick() {
        assertEquals(1, ItemAbilities.scaled(1, 5.0));
    }

    @Test
    void negativeHasteIsIgnoredRatherThanSlowingAnAbility() {
        assertEquals(240, ItemAbilities.scaled(240, -0.5));
    }
}
