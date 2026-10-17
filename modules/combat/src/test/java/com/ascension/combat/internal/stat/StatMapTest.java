package com.ascension.combat.internal.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ascension.combat.api.stat.CombatStats;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/**
 * Mob stats' format and merge rule (docs/technical/item-stats.md section 9): a mob type's own
 * entry beats its tier tag's, one stat at a time, whatever order the files load in. Pure logic.
 */
class StatMapTest {

    private static StatMap parse(String json) {
        return StatMap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void readsStatsByName() {
        StatMap stats = parse("{ \"armor\": 100, \"magic_resist\": 40 }");

        assertEquals(100.0, stats.stats().get(CombatStats.ARMOR));
        assertEquals(40.0, stats.stats().get(CombatStats.MAGIC_RESIST));
    }

    @Test
    void aTypesOwnEntryBeatsItsTierWhicheverLoadsFirst() {
        StatMap tier = parse("{ \"armor\": 100, \"magic_resist\": 40 }");
        StatMap warden = parse("{ \"armor\": 120 }");

        for (StatMap merged : new StatMap[] {StatMap.merge(tier, false, warden, true), StatMap.merge(warden, true, tier, false)}) {
            assertEquals(120.0, merged.stats().get(CombatStats.ARMOR));
            assertEquals(40.0, merged.stats().get(CombatStats.MAGIC_RESIST));
        }
    }

    @Test
    void aTypesOwnEntryKeepsWinningAcrossTwoTiers() {
        StatMap golem = parse("{ \"armor\": 60 }");
        StatMap heavy = parse("{ \"armor\": 50, \"magic_resist\": 20 }");
        StatMap boss = parse("{ \"armor\": 100, \"magic_resist\": 40 }");

        StatMap merged = StatMap.merge(StatMap.merge(golem, true, heavy, false), false, boss, false);

        assertEquals(60.0, merged.stats().get(CombatStats.ARMOR));
        assertEquals(40.0, merged.stats().get(CombatStats.MAGIC_RESIST), "between two tiers, the later wins");
    }
}
