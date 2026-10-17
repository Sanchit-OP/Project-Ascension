package com.ascension.combat.api.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** How a modifier id finds its source's label on the character sheet. Pure lookup. */
class StatSourcesTest {

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Test
    void theLongestMatchingPrefixWins() {
        StatSources.register(id("statsourcestest", "growth/"), Component.literal("Growth"));
        StatSources.register(id("statsourcestest", "growth/warden/"), Component.literal("The Warden"));

        assertEquals("The Warden", StatSources.labelFor(id("statsourcestest", "growth/warden/health")).orElseThrow().getString());
        assertEquals("Growth", StatSources.labelFor(id("statsourcestest", "growth/gate_moon")).orElseThrow().getString());
    }

    @Test
    void aPrefixOnlyMatchesItsOwnNamespace() {
        StatSources.register(id("statsourcestest_a", "rune/"), Component.literal("Runes"));

        assertTrue(StatSources.labelFor(id("statsourcestest_b", "rune/conquerors_resolve")).isEmpty());
    }
}
