package com.ascension.core.internal.loot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Which loot table ids count as a chest's: the ones under {@code chests/}, whoever owns them. */
class ChestTableConditionTest {

    private static ResourceLocation id(String text) {
        return ResourceLocation.parse(text);
    }

    @Test
    void chestTablesOfAnyOwnerCount() {
        assertTrue(ChestTableCondition.isChestTable(id("minecraft:chests/simple_dungeon")));
        assertTrue(ChestTableCondition.isChestTable(id("minecraft:chests/village/village_armorer")));
        assertTrue(ChestTableCondition.isChestTable(id("cataclysm:chests/acropolis_treasure")));
        assertTrue(ChestTableCondition.isChestTable(id("ascension_worlds:chests/anything")));
    }

    @Test
    void blocksMobsAndFishingDoNot() {
        assertFalse(ChestTableCondition.isChestTable(id("minecraft:blocks/stone")));
        assertFalse(ChestTableCondition.isChestTable(id("minecraft:entities/zombie")));
        assertFalse(ChestTableCondition.isChestTable(id("minecraft:gameplay/fishing")));
        assertFalse(ChestTableCondition.isChestTable(id("minecraft:archaeology/desert_well")));
    }

    @Test
    void aTableOnlyNamedLikeAChestDoesNot() {
        assertFalse(ChestTableCondition.isChestTable(id("mod:mychests/loot")));
        assertFalse(ChestTableCondition.isChestTable(id("mod:chests")));
    }
}
