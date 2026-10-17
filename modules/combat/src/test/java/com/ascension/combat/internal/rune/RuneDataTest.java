package com.ascension.combat.internal.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.combat.api.rune.RunePath;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Rune data files and saved pages as they are read: a rune's checks, and pages saved before
 * 2026-10-17 still loading, with their runes renamed. Pure decoding; says nothing about the game
 * (ADR-0008).
 */
class RuneDataTest {

    private static DataResult<RuneDefinition> rune(String json) {
        return RuneDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static final String MOMENTUM = """
            {"path": "vanguard", "slot": "minor", "row": 0, "position": 1,
             "icon": "ascension_combat:rune/momentum", "effect": "ascension_combat:momentum",
             "values": {"max_stacks": 5, "heal_per_stack": 0.01, "chain_seconds": 2}}""";

    @Test
    void aRuneFileReads() {
        RuneDefinition rune = rune(MOMENTUM).getOrThrow();
        assertEquals(Optional.of(RunePath.VANGUARD), rune.path());
        assertEquals(RuneSlot.MINOR, rune.slot());
        assertEquals(5, rune.count("max_stacks", 0));
        assertEquals(40L, rune.ticks("chain_seconds", 0));
        assertEquals(7.0, rune.value("missing", 7.0));
        assertFalse(rune.world());
        assertFalse(rune.grows());
    }

    @Test
    void badRuneFilesAreReportedNotCrashed() {
        assertTrue(rune("""
                {"slot": "minor", "icon": "a:b", "effect": "a:b"}""").isError(), "a minor needs a path");
        assertTrue(rune("""
                {"path": "reaver", "slot": "minor", "row": 2, "icon": "a:b", "effect": "a:b"}""").isError(),
                "minors have two rows");
        assertTrue(rune("""
                {"path": "reaver", "slot": "shard", "icon": "a:b", "effect": "a:b"}""").isError(),
                "a shard has no path");
        assertTrue(rune("""
                {"path": "reaver", "slot": "minor", "world": true, "icon": "a:b", "effect": "a:b"}""").isError(),
                "only keystones can be world keystones");
        assertTrue(rune("""
                {"path": "lancer", "slot": "keystone", "icon": "a:b", "effect": "a:b"}""").isError(),
                "unknown path");
    }

    @Test
    void aRunesNumbersMustReadItsOwnValues() {
        assertTrue(rune("""
                {"path": "vanguard", "slot": "minor", "icon": "a:b", "effect": "a:b", "values": {"hits": 3},
                 "numbers": [{"text": "x", "parts": [{"value": "hits"}]}]}""").isSuccess());
        assertTrue(rune("""
                {"path": "vanguard", "slot": "minor", "icon": "a:b", "effect": "a:b", "values": {"hits": 3},
                 "numbers": [{"text": "x", "parts": [{"value": "window"}]}]}""").isError(),
                "a card line can't show a number the rune doesn't have");
        assertTrue(rune("""
                {"path": "vanguard", "slot": "minor", "icon": "a:b", "effect": "a:b", "values": {"hits": 3},
                 "numbers": [{"text": "x", "parts": [{"value": "hits", "stat": "luck"}]}]}""").isError(),
                "the colour must be a stat the stat system knows");
    }

    @Test
    void theShippedVanguardRunesReadTheirOwnFiles() throws Exception {
        for (String name : List.of("conquerors_resolve", "cadence", "vein_breaker", "second_wind", "momentum",
                "blooded", "relentless_strikes", "killing_surge", "giant_slayer")) {
            try (var in = RuneDataTest.class.getResourceAsStream(
                    "/data/ascension_combat/ascension_combat/rune/" + name + ".json")) {
                assertTrue(in != null, name + " is shipped");
                RuneDefinition rune = RuneDefinition.CODEC.parse(JsonOps.INSTANCE,
                        JsonParser.parseReader(new java.io.InputStreamReader(in))).getOrThrow();
                assertEquals(Optional.of(RunePath.VANGUARD), rune.path(), name);
                assertFalse(rune.numbers().isEmpty(), name + " has numbers for its card");
            }
        }
    }

    @Test
    void aShardFileReads() {
        RuneDefinition shard = rune("""
                {"slot": "shard", "row": 2, "icon": "a:b", "effect": "a:b", "values": {"amount": 2}}""").getOrThrow();
        assertEquals(Optional.empty(), shard.path());
        assertEquals(2, shard.row());
    }

    @Test
    void anOldPageLoadsWithItsRunesRenamed() {
        JsonElement old = JsonParser.parseString("""
                {"primary_path": "VANGUARD",
                 "primary_minors": ["ascension_combat:vanguard_momentum", "ascension_combat:vanguard_killing_surge"],
                 "secondary_minors": []}""");
        RunePageState state = RunePageState.SAVED_CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(Optional.of(RunePath.VANGUARD), state.page().primary());
        assertTrue(state.has(ResourceLocation.fromNamespaceAndPath("ascension_combat", "momentum")));
        assertTrue(state.has(ResourceLocation.fromNamespaceAndPath("ascension_combat", "killing_surge")));
        assertEquals(Optional.empty(), state.page().keystone());
    }

    @Test
    void aPageSurvivesSavingAndLoading() {
        RunePageState state = new RunePageState();
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("ascension_combat", "cadence");
        ResourceLocation blooded = ResourceLocation.fromNamespaceAndPath("ascension_combat", "blooded");
        state.set(new RunePage(Optional.of(RunePath.VANGUARD), Optional.of(key), List.of(blooded),
                Optional.of(RunePath.BULWARK), List.of(), List.of()));
        state.grow(blooded, ResourceLocation.withDefaultNamespace("zombie"), 12);

        JsonElement saved = RunePageState.SAVED_CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        RunePageState loaded = RunePageState.SAVED_CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertTrue(loaded.page().sameAs(state.page()));
        assertEquals(1, loaded.growth(blooded));
    }

    @Test
    void growthCountsEachKindOnceUpToItsCap() {
        RunePageState state = new RunePageState();
        ResourceLocation roots = ResourceLocation.fromNamespaceAndPath("ascension_combat", "deep_roots");
        state.set(new RunePage(Optional.of(RunePath.BULWARK), Optional.empty(), List.of(roots),
                Optional.empty(), List.of(), List.of()));
        ResourceLocation zombie = ResourceLocation.withDefaultNamespace("zombie");
        assertTrue(state.grow(roots, zombie, 2));
        assertFalse(state.grow(roots, zombie, 2), "a kind counts once: a mob farm can't fill it");
        assertTrue(state.grow(roots, ResourceLocation.withDefaultNamespace("skeleton"), 2));
        assertFalse(state.grow(roots, ResourceLocation.withDefaultNamespace("spider"), 2), "capped");
        assertEquals(2, state.growth(roots));
    }

    @Test
    void aRuneTakenOffThePageLosesItsGrowth() {
        RunePageState state = new RunePageState();
        ResourceLocation roots = ResourceLocation.fromNamespaceAndPath("ascension_combat", "deep_roots");
        RunePage withRoots = new RunePage(Optional.of(RunePath.BULWARK), Optional.empty(), List.of(roots),
                Optional.empty(), List.of(), List.of());
        state.set(withRoots);
        state.grow(roots, ResourceLocation.withDefaultNamespace("zombie"), 12);
        state.set(RunePage.EMPTY);
        state.set(withRoots);
        assertEquals(0, state.growth(roots));
        assertFalse(state.grow(ResourceLocation.fromNamespaceAndPath("ascension_combat", "blooded"),
                ResourceLocation.withDefaultNamespace("zombie"), 12), "only a rune on the page grows");
    }
}
