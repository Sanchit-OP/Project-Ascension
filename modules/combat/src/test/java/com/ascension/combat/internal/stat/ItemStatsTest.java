package com.ascension.combat.internal.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.api.stat.CombatStats;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.world.entity.EquipmentSlotGroup;
import org.junit.jupiter.api.Test;

/**
 * The item-stats file format and its merge rule (docs/technical/item-stats.md section 4): an entry
 * naming the item beats a tag's, one field at a time, whatever order the files load in. Pure logic;
 * says nothing about stats reaching a player in game (ADR-0008).
 */
class ItemStatsTest {

    private static ItemStats parse(String json) {
        return ItemStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void readsEveryPart() {
        ItemStats stats = parse("""
                { "weapon": { "damage": 7, "speed": 1.6 },
                  "stats": { "armor": 15, "omnivamp": 8 },
                  "slot": "feet" }
                """);

        assertEquals(Optional.of(7.0), stats.weaponDamage());
        assertEquals(Optional.of(1.6), stats.weaponSpeed());
        assertEquals(15.0, stats.stats().get(CombatStats.ARMOR));
        assertEquals(8.0, stats.stats().get(CombatStats.OMNIVAMP), "percent stats stay as written");
        assertEquals(Optional.of(EquipmentSlotGroup.FEET), stats.slot());
    }

    @Test
    void everyPartIsOptional() {
        ItemStats stats = parse("{}");

        assertTrue(stats.weaponDamage().isEmpty());
        assertTrue(stats.stats().isEmpty());
        assertTrue(stats.slot().isEmpty());
    }

    @Test
    void anUnknownStatFailsRatherThanVanishing() {
        assertTrue(ItemStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"stats\": { \"armour\": 5 } }"))
                .error().isPresent());
    }

    @Test
    void anItemEntryBeatsAnEarlierTag() {
        ItemStats tag = parse("{ \"stats\": { \"armor\": 5, \"lethality\": 3 } }");
        ItemStats item = parse("{ \"stats\": { \"armor\": 20 } }");

        ItemStats merged = ItemStats.merge(tag, false, item, true);

        assertEquals(20.0, merged.stats().get(CombatStats.ARMOR));
        assertEquals(3.0, merged.stats().get(CombatStats.LETHALITY), "the tag's other stats stay");
    }

    @Test
    void anItemEntryBeatsALaterTag() {
        ItemStats item = parse("{ \"stats\": { \"armor\": 20 } }");
        ItemStats tag = parse("{ \"stats\": { \"armor\": 5, \"lethality\": 3 } }");

        ItemStats merged = ItemStats.merge(item, true, tag, false);

        assertEquals(20.0, merged.stats().get(CombatStats.ARMOR));
        assertEquals(3.0, merged.stats().get(CombatStats.LETHALITY));
    }

    @Test
    void anItemEntryKeepsWinningAcrossSeveralLaterTags() {
        // NeoForge passes the next merge only the newest entry's source, so after the first tag
        // the merged value itself has to remember that armor came from the item.
        ItemStats item = parse("{ \"stats\": { \"armor\": 20 } }");
        ItemStats firstTag = parse("{ \"stats\": { \"armor\": 5 } }");
        ItemStats secondTag = parse("{ \"stats\": { \"armor\": 1, \"health\": 4 } }");

        ItemStats merged = ItemStats.merge(ItemStats.merge(item, true, firstTag, false), false, secondTag, false);

        assertEquals(20.0, merged.stats().get(CombatStats.ARMOR));
        assertEquals(4.0, merged.stats().get(CombatStats.HEALTH));
    }

    @Test
    void betweenEqualsTheLaterWins() {
        ItemStats earlier = parse("{ \"stats\": { \"armor\": 5 } }");
        ItemStats later = parse("{ \"stats\": { \"armor\": 9 } }");

        assertEquals(9.0, ItemStats.merge(earlier, false, later, false).stats().get(CombatStats.ARMOR), "two tags");
        assertEquals(9.0, ItemStats.merge(earlier, true, later, true).stats().get(CombatStats.ARMOR), "two items");
    }

    @Test
    void weaponFieldsAndSlotMergeOneAtATime() {
        ItemStats tag = parse("{ \"weapon\": { \"damage\": 6, \"speed\": 1.2 }, \"slot\": \"mainhand\" }");
        ItemStats item = parse("{ \"weapon\": { \"damage\": 9 }, \"slot\": \"hand\" }");

        ItemStats merged = ItemStats.merge(item, true, tag, false);

        assertEquals(Optional.of(9.0), merged.weaponDamage());
        assertEquals(Optional.of(1.2), merged.weaponSpeed());
        assertEquals(Optional.of(EquipmentSlotGroup.HAND), merged.slot());
    }

    @Test
    void percentStatsBecomeFractionsOnlyOnTheModifier() {
        assertEquals(0.08, CombatStats.OMNIVAMP.toAmount(8), 1e-12);
        assertEquals(15.0, CombatStats.ARMOR.toAmount(15), 1e-12);
        assertEquals(8.0, CombatStats.OMNIVAMP.toWritten(0.08), 1e-12);
    }
}
