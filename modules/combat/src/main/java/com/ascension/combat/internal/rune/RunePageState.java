package com.ascension.combat.internal.rune;

import com.ascension.combat.api.rune.RunePath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's saved rune page (docs/technical/combat-api.md section 7): a primary path, whose one
 * keystone (see {@link RuneCatalog}) is granted automatically the moment that path is chosen --
 * only one keystone per path exists so far, so there is nothing further to pick there -- plus up
 * to two of that path's minors; and a secondary path contributing up to two of its own minors,
 * with no keystone.
 *
 * <p>Attached to the player and serialized ({@link #CODEC}): unlike this module's fight-local
 * state (docs/technical/combat-api.md section 5), a rune choice is a build decision meant to
 * survive a logout, the same reasoning {@code PlanetArrivalMemory} in {@code ascension-worlds}
 * gives for serializing itself.
 */
public final class RunePageState {

    public static final Codec<RunePageState> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    RunePath.CODEC.optionalFieldOf("primary_path")
                            .forGetter(state -> Optional.ofNullable(state.primaryPath)),
                    ResourceLocation.CODEC.listOf().fieldOf("primary_minors")
                            .forGetter(state -> List.copyOf(state.primaryMinors)),
                    RunePath.CODEC.optionalFieldOf("secondary_path")
                            .forGetter(state -> Optional.ofNullable(state.secondaryPath)),
                    ResourceLocation.CODEC.listOf().fieldOf("secondary_minors")
                            .forGetter(state -> List.copyOf(state.secondaryMinors)))
            .apply(instance, (primary, primaryMinors, secondary, secondaryMinors) -> new RunePageState(
                    primary.orElse(null),
                    new HashSet<>(primaryMinors),
                    secondary.orElse(null),
                    new HashSet<>(secondaryMinors))));

    private RunePath primaryPath;
    private RunePath secondaryPath;
    private final Set<ResourceLocation> primaryMinors;
    private final Set<ResourceLocation> secondaryMinors;

    public RunePageState() {
        this(null, Set.of(), null, Set.of());
    }

    private RunePageState(
            RunePath primaryPath,
            Set<ResourceLocation> primaryMinors,
            RunePath secondaryPath,
            Set<ResourceLocation> secondaryMinors) {
        this.primaryPath = primaryPath;
        this.primaryMinors = new HashSet<>(primaryMinors);
        this.secondaryPath = secondaryPath;
        this.secondaryMinors = new HashSet<>(secondaryMinors);
    }

    public RunePath primaryPath() {
        return primaryPath;
    }

    public RunePath secondaryPath() {
        return secondaryPath;
    }

    public Set<ResourceLocation> primaryMinors() {
        return Set.copyOf(primaryMinors);
    }

    public Set<ResourceLocation> secondaryMinors() {
        return Set.copyOf(secondaryMinors);
    }

    public boolean hasPrimaryPath(RunePath path) {
        return primaryPath == path;
    }

    /** Whether either side of the page has picked the minor rune {@code id}. */
    public boolean hasMinor(ResourceLocation id) {
        return primaryMinors.contains(id) || secondaryMinors.contains(id);
    }

    /**
     * Replaces the whole page in one step, validated against {@link RuneCatalog} rather than
     * trusting the client's own selection -- the server-side handler for
     * {@link com.ascension.combat.internal.net.SetRunePagePayload} calls this and nothing else.
     * Returns {@code false} and leaves the page unchanged if the requested selection breaks a
     * rule: no primary path, primary and secondary the same path, more than two minors on
     * either side, or a minor that doesn't belong to the path it's claimed under.
     */
    public boolean trySetPage(
            RunePath newPrimaryPath,
            Set<ResourceLocation> newPrimaryMinors,
            RunePath newSecondaryPath,
            Set<ResourceLocation> newSecondaryMinors) {
        if (newPrimaryPath == null || newPrimaryPath == newSecondaryPath) {
            return false;
        }
        if (newPrimaryMinors.size() > 2 || newSecondaryMinors.size() > 2) {
            return false;
        }
        if (newSecondaryPath == null && !newSecondaryMinors.isEmpty()) {
            return false;
        }
        if (!validMinorsFor(newPrimaryPath).containsAll(newPrimaryMinors)) {
            return false;
        }
        if (newSecondaryPath != null && !validMinorsFor(newSecondaryPath).containsAll(newSecondaryMinors)) {
            return false;
        }

        this.primaryPath = newPrimaryPath;
        this.primaryMinors.clear();
        this.primaryMinors.addAll(newPrimaryMinors);
        this.secondaryPath = newSecondaryPath;
        this.secondaryMinors.clear();
        this.secondaryMinors.addAll(newSecondaryMinors);
        return true;
    }

    private static Set<ResourceLocation> validMinorsFor(RunePath path) {
        return RuneCatalog.minorsOf(path).stream().map(RuneDefinition::id).collect(Collectors.toSet());
    }
}
