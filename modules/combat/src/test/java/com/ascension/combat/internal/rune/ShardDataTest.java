package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The shipped stat shards (docs/technical/rune-system.md section 5), read from their own files. Pure decoding (ADR-0008). */
class ShardDataTest {

    private static final List<String> SHARDS = List.of(
            "shard_offence_attack_damage", "shard_offence_attack_speed", "shard_offence_move_speed",
            "shard_flex_attack_damage", "shard_flex_armor", "shard_flex_magic_resist",
            "shard_defence_health", "shard_defence_armor", "shard_defence_magic_resist");

    private static RuneDefinition read(String name) throws Exception {
        try (var in = ShardDataTest.class.getResourceAsStream("/data/ascension_combat/ascension_combat/rune/" + name + ".json")) {
            assertTrue(in != null, name + " is shipped");
            return RuneDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(in))).getOrThrow();
        }
    }

    @Test
    void everyShardReadsAndGivesAStatTheStatSystemKnows() throws Exception {
        for (String name : SHARDS) {
            RuneDefinition shard = read(name);
            assertEquals(RuneSlot.SHARD, shard.slot(), name);
            assertTrue(ShardEffects.statOf(shard).isPresent(), name + " names a known stat");
            assertTrue(shard.value("amount", 0.0) > 0.0, name + " gives something");
            assertTrue(shard.icon().getPath().startsWith("shard/"), name + " uses a shard sprite");
        }
    }

    @Test
    void eachRowHasThreeShardsInThreePlaces() throws Exception {
        for (int row = 0; row < RuneDefinition.SHARD_ROWS; row++) {
            Set<Integer> places = new HashSet<>();
            for (String name : SHARDS) {
                RuneDefinition shard = read(name);
                if (shard.row() == row) {
                    places.add(shard.position());
                }
            }
            assertEquals(Set.of(0, 1, 2), places, "row " + row);
        }
    }

    @Test
    void theStatsAreTheOnesTheDesignListsForEachRow() throws Exception {
        assertEquals("attack_damage", ShardEffects.statOf(read("shard_offence_attack_damage")).get().name());
        assertEquals("attack_speed", ShardEffects.statOf(read("shard_offence_attack_speed")).get().name());
        assertEquals(8.0, read("shard_offence_attack_speed").value("amount", 0), "+8% attack speed, written as 8");
        assertEquals("health", ShardEffects.statOf(read("shard_defence_health")).get().name());
        assertEquals(2.0, read("shard_defence_health").value("amount", 0));
    }
}
