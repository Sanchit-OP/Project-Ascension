package com.ascension.worlds.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the crash a live player actually hit 2026-09-06: a brand-new
 * {@link PlanetArrivalMemory} works fine (its map is a real {@code HashMap}), but one rebuilt by
 * decoding previously-saved data threw {@code UnsupportedOperationException} the moment
 * {@link PlanetArrivalMemory#recordAscent} tried to write into it, because
 * {@code Codec.unboundedMap}'s decode side hands back Guava's immutable {@code ImmutableMap}. See
 * {@code plans/m2-worlds.md}'s M2.6 section for the crash log this reproduces without needing a
 * server or a save/reload cycle to trigger it.
 */
class PlanetArrivalMemoryTest {

    private static final ResourceLocation EARTH = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "moon");

    @Test
    void aDecodedInstanceCanStillRecordAnAscent() {
        PlanetArrivalMemory original = new PlanetArrivalMemory();
        original.recordAscent(EARTH, new BlockPos(1, 2, 3), 90.0F);

        PlanetArrivalMemory decoded = roundTrip(original);

        assertDoesNotThrow(() -> decoded.recordAscent(MOON, new BlockPos(4, 5, 6), 0.0F));
    }

    @Test
    void decodingPreservesAlreadyRecordedAscents() {
        PlanetArrivalMemory original = new PlanetArrivalMemory();
        original.recordAscent(EARTH, new BlockPos(10, 20, 30), 45.0F);

        PlanetArrivalMemory decoded = roundTrip(original);

        PlanetArrivalMemory.DeparturePoint point = decoded.lastAscentFrom(EARTH);
        assertEquals(new BlockPos(10, 20, 30), point.pos());
        assertEquals(45.0F, point.yaw());
    }

    @Test
    void aDimensionNeverAscendedFromReturnsNull() {
        PlanetArrivalMemory memory = new PlanetArrivalMemory();
        assertNull(memory.lastAscentFrom(MOON));
    }

    private static PlanetArrivalMemory roundTrip(PlanetArrivalMemory memory) {
        JsonElement encoded = PlanetArrivalMemory.CODEC.encodeStart(JsonOps.INSTANCE, memory).getOrThrow();
        return PlanetArrivalMemory.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }
}
