package com.ascension.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the shared world environment record and its serialised form.
 *
 * <p>The codec tests matter more than they look. This is the format planets are authored in, so
 * it is a compatibility surface for datapack authors as much as the Java type is for mod authors
 * &mdash; and unlike the Java type, it is easy to break by accident while refactoring.
 */
class WorldEnvironmentTest {

    @Test
    void drainMultiplierMustBeFiniteAndNonNegative() {
        assertThrows(IllegalArgumentException.class, () -> new WorldEnvironment(false, -1.0f));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldEnvironment(false, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldEnvironment(false, Float.POSITIVE_INFINITY));
    }

    @Test
    void constantsAreWhatTheyClaim() {
        assertTrue(WorldEnvironment.EARTHLIKE.breathable());
        assertEquals(0.0f, WorldEnvironment.EARTHLIKE.drainMultiplier());

        assertFalse(WorldEnvironment.AIRLESS.breathable());
        assertEquals(1.0f, WorldEnvironment.AIRLESS.drainMultiplier());
    }

    @Test
    void hostileIsUnbreathableAtTheGivenRate() {
        WorldEnvironment corrosive = WorldEnvironment.hostile(2.0f);
        assertFalse(corrosive.breathable());
        assertEquals(2.0f, corrosive.drainMultiplier());
    }

    @Test
    void emptyJsonIsAValidEarthlikeEnvironment() {
        // A world with nothing to say about its air is Earth. This is what lets a planet.json
        // omit the environment block entirely, which earth.json does.
        assertEquals(WorldEnvironment.EARTHLIKE, decode("{}"));
    }

    @Test
    void bothFieldsDecode() {
        assertEquals(WorldEnvironment.AIRLESS,
                decode("{\"breathable\": false, \"drain_multiplier\": 1.0}"));
        assertEquals(WorldEnvironment.hostile(2.5f),
                decode("{\"breathable\": false, \"drain_multiplier\": 2.5}"));
    }

    @Test
    void fieldNamesAreSnakeCase() {
        // Guarding the wire format, not the behaviour: `drainMultiplier` in JSON would silently
        // fall back to the default rather than fail, so a rename here breaks every datapack that
        // already exists and does so without an error message.
        WorldEnvironment wrongCase = decode("{\"breathable\": false, \"drainMultiplier\": 9.0}");
        assertEquals(0.0f, wrongCase.drainMultiplier());
    }

    @Test
    void roundTrips() {
        WorldEnvironment original = WorldEnvironment.hostile(1.75f);
        var encoded = WorldEnvironment.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        WorldEnvironment decoded = WorldEnvironment.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void invalidDrainIsADecodeErrorRatherThanACrash() {
        // The record constructor throws, and the codec has to surface that as a failed parse so a
        // bad datapack reports itself at load instead of at some later moment.
        var result = WorldEnvironment.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"breathable\": false, \"drain_multiplier\": -3.0}"));
        assertTrue(result.isError(), "expected a decode error, got " + result.result());
    }

    private static WorldEnvironment decode(String json) {
        return WorldEnvironment.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow();
    }
}
