package com.ascension.combat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import org.junit.jupiter.api.Test;

/** How the tree screen writes a stat's number and which icon it gets. The name lookup needs the game. */
class GrowthDisplayTest {

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static final ResourceLocation HEALTH = id("minecraft", "generic.max_health");
    private static final ResourceLocation ATTACK_SPEED = id("minecraft", "generic.attack_speed");
    private static final ResourceLocation OMNIVAMP = id("ascension_combat", "omnivamp");

    @Test
    void aWholeNumberHasNoDecimalPoint() {
        assertEquals("+4", GrowthDisplay.number(HEALTH, Operation.ADD_VALUE, 4));
    }

    @Test
    void aFractionKeepsItsDecimalsAndNoTrailingZeros() {
        assertEquals("+0.25", GrowthDisplay.number(id("minecraft", "generic.attack_damage"), Operation.ADD_VALUE, 0.25));
        assertEquals("+0.5", GrowthDisplay.number(id("minecraft", "player.block_interaction_range"), Operation.ADD_VALUE, 0.5));
    }

    @Test
    void aMultiplierReadsAsAPercent() {
        assertEquals("+3%", GrowthDisplay.number(ATTACK_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.03));
        assertEquals("+12%", GrowthDisplay.number(ATTACK_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.12));
    }

    @Test
    void aStatStoredAsAFractionReadsAsAPercentToo() {
        assertEquals("+1%", GrowthDisplay.number(OMNIVAMP, Operation.ADD_VALUE, 0.01));
        assertEquals("+8%", GrowthDisplay.number(id("ascension_combat", "armor_penetration_percent"), Operation.ADD_VALUE, 0.08));
    }

    @Test
    void aNegativeNumberKeepsItsSign() {
        assertEquals("-2", GrowthDisplay.number(HEALTH, Operation.ADD_VALUE, -2));
    }

    @Test
    void everyStatTheTreeUsesHasAnIcon() {
        for (String attribute : new String[] {
                "minecraft:generic.max_health", "ascension_combat:armor", "ascension_combat:magic_resist",
                "minecraft:generic.attack_damage", "ascension_combat:lethality",
                "ascension_combat:armor_penetration_percent", "ascension_combat:magic_penetration_percent",
                "minecraft:generic.attack_speed", "ascension_combat:omnivamp", "minecraft:generic.movement_speed",
                "minecraft:player.block_break_speed", "minecraft:player.block_interaction_range",
                "minecraft:generic.safe_fall_distance", "minecraft:generic.step_height"}) {
            ResourceLocation id = ResourceLocation.parse(attribute);
            assertEquals(true, GrowthDisplay.glyph(id) >= 0, attribute + " has no icon");
        }
        assertEquals(GrowthDisplay.NO_GLYPH, GrowthDisplay.glyph(id("othermod", "luck")));
    }
}
