package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Which advancement a lit gate becomes. Pure naming: that a lit gate is actually given to a
 * player only a running server shows (ADR-0008).
 */
class GateAdvancementsTest {

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Test
    void earthAndTheNetherKeepTheirVanillaNames() {
        assertEquals(id("ascension_worlds", "gates/overworld"), GateAdvancements.advancementFor(id("minecraft", "overworld")));
        assertEquals(id("ascension_worlds", "gates/the_nether"), GateAdvancements.advancementFor(id("minecraft", "the_nether")));
    }

    @Test
    void ourOwnPlanetsAreNamedByPath() {
        assertEquals(id("ascension_worlds", "gates/moon"), GateAdvancements.advancementFor(id("ascension_worlds", "moon")));
    }

    @Test
    void anotherPacksPlanetKeepsItsNamespace() {
        assertEquals(id("ascension_worlds", "gates/otherpack/moon"),
                GateAdvancements.advancementFor(id("otherpack", "moon")));
    }
}
