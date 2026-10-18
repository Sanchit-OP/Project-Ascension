package com.ascension.hud.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class HudVisibilityTest {

    private static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static final List<ResourceLocation> IDS =
            List.of(id("a", "vitals"), id("b", "stat_block"), id("b", "rune_chips"));

    @Test
    void nothingHiddenByDefault() {
        boolean[] out = new boolean[3];
        HudVisibility.compute(IDS, List.of(), out);
        assertArrayEquals(new boolean[] {false, false, false}, out);
    }

    @Test
    void namedElementsAreHidden() {
        boolean[] out = new boolean[3];
        HudVisibility.compute(IDS, List.of("b:stat_block"), out);
        assertArrayEquals(new boolean[] {false, true, false}, out);
    }

    @Test
    void unknownAndMalformedEntriesAreIgnored() {
        boolean[] out = new boolean[3];
        HudVisibility.compute(IDS, List.of("nope:missing", "Not A Valid Id!!", "", " a:vitals "), out);
        assertArrayEquals(new boolean[] {true, false, false}, out);
    }

    @Test
    void aStaleResultIsClearedBeforeUse() {
        boolean[] out = {true, true, true};
        HudVisibility.compute(IDS, List.of(), out);
        assertArrayEquals(new boolean[] {false, false, false}, out);
    }
}
