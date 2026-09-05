package com.ascension.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.WorldEnvironment;
import com.ascension.core.api.WorldEnvironmentSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the shared world environment registry.
 *
 * <p>Unit tests are appropriate here and almost nowhere else in this project: per ADR-0008 they
 * are never evidence for anything touching world state, and this class touches none. It is a map,
 * a sort and a loop. Everything it does is decidable without a running game, which is exactly the
 * category ADR-0008 says tests are for.
 *
 * <p>ADR-0011 is why these exist at all: once {@code core} stopped being an empty stub and became
 * a semver contract every other module depends on, "it is just infrastructure" stopped being a
 * reason not to test it.
 */
class WorldEnvironmentsTest {

    private static ResourceKey<Level> dim(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("test", path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    /** A source that answers for exactly one dimension. */
    private static WorldEnvironmentSource owning(ResourceKey<Level> owned, WorldEnvironment env) {
        return dimension -> dimension.equals(owned) ? Optional.of(env) : Optional.empty();
    }

    @Test
    void unknownDimensionIsUnknownRatherThanBreathable() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.freeze();

        // The distinction matters: "nobody knows" is a different answer from "it is fine here",
        // and deciding which one means what is the consumer's job, not this module's.
        assertTrue(registry.resolve(dim("nowhere")).isEmpty());
    }

    @Test
    void aSourceAnswersForTheDimensionItOwns() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.AIRLESS));
        registry.freeze();

        assertEquals(Optional.of(WorldEnvironment.AIRLESS), registry.resolve(dim("moon")));
        assertTrue(registry.resolve(dim("mars")).isEmpty());
    }

    @Test
    void queryingBeforeFreezeAnswersUnknownRatherThanThrowing() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.AIRLESS));

        // Deliberately not an exception. A load-order mistake in somebody else's mod should not
        // crash the game; it should be visible in the log and answer safely.
        assertTrue(registry.resolve(dim("moon")).isEmpty());
    }

    @Test
    void registeringAfterFreezeThrows() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.freeze();

        assertThrows(IllegalStateException.class,
                () -> registry.add(id("late"), owning(dim("late"), WorldEnvironment.AIRLESS)));
    }

    @Test
    void duplicateIdThrows() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.AIRLESS));

        assertThrows(IllegalArgumentException.class,
                () -> registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.EARTHLIKE)));
    }

    @Test
    void conflictsResolveByIdAndNotByRegistrationOrder() {
        WorldEnvironments first = new WorldEnvironments();
        first.add(id("aaa"), owning(dim("contested"), WorldEnvironment.AIRLESS));
        first.add(id("zzz"), owning(dim("contested"), WorldEnvironment.EARTHLIKE));
        first.freeze();

        // Same two sources, registered in the opposite order.
        WorldEnvironments second = new WorldEnvironments();
        second.add(id("zzz"), owning(dim("contested"), WorldEnvironment.EARTHLIKE));
        second.add(id("aaa"), owning(dim("contested"), WorldEnvironment.AIRLESS));
        second.freeze();

        // This is the whole reason ids are mandatory. Without it the winner would depend on mod
        // load order, which is a bug that only reproduces on someone else's machine.
        assertEquals(first.resolve(dim("contested")), second.resolve(dim("contested")));
        assertEquals(Optional.of(WorldEnvironment.AIRLESS), first.resolve(dim("contested")));
    }

    @Test
    void sourcesAreListedInResolutionOrder() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("zzz"), owning(dim("a"), WorldEnvironment.AIRLESS));
        registry.add(id("aaa"), owning(dim("b"), WorldEnvironment.AIRLESS));
        registry.add(id("mmm"), owning(dim("c"), WorldEnvironment.AIRLESS));
        registry.freeze();

        assertEquals(List.of(id("aaa"), id("mmm"), id("zzz")), registry.ids());
    }

    @Test
    void listIsEmptyBeforeFreeze() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.AIRLESS));

        assertTrue(registry.ids().isEmpty());
    }

    @Test
    void freezeIsIdempotent() {
        WorldEnvironments registry = new WorldEnvironments();
        registry.add(id("moon"), owning(dim("moon"), WorldEnvironment.AIRLESS));
        registry.freeze();
        registry.freeze();

        assertEquals(Optional.of(WorldEnvironment.AIRLESS), registry.resolve(dim("moon")));
        assertEquals(1, registry.ids().size());
    }
}
