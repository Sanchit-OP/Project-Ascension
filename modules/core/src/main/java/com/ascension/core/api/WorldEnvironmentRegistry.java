package com.ascension.core.api;

import com.ascension.core.internal.WorldEnvironments;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Ask what a dimension is like, or say what one of yours is like.
 *
 * <p>The meeting point ADR-0011 describes: whoever owns a world registers a
 * {@link WorldEnvironmentSource}, whoever cares about worlds queries here, and neither module
 * needs to know the other exists.
 *
 * <p>Register during mod setup. The registry closes once mod loading completes, so that queries
 * never sort, never allocate, and never see a half-built list; registering afterwards throws.
 *
 * <p>Every registration takes a stable id. It costs the caller nothing and buys two things worth
 * more: a conflict between two sources claiming the same dimension can be reported by name, and
 * the winner is decided by id ordering rather than by whichever mod happened to load first.
 */
public final class WorldEnvironmentRegistry {

    private WorldEnvironmentRegistry() {
    }

    /**
     * Contribute knowledge of your own dimensions.
     *
     * <p>Safe to call from any thread during setup, which matters because
     * {@code FMLCommonSetupEvent} runs mods in parallel.
     *
     * @param id     stable identifier, conventionally {@code yourmod:what_it_covers}
     * @param source consulted for dimensions it owns
     * @throws IllegalStateException    if mod loading has already completed
     * @throws IllegalArgumentException if {@code id} is already registered
     */
    public static void register(ResourceLocation id, WorldEnvironmentSource source) {
        WorldEnvironments.get().add(id, source);
    }

    /**
     * What this dimension is like, according to whoever owns it.
     *
     * <p><strong>Empty is not "breathable", it is "nobody knows".</strong> The distinction is the
     * point: this registry does not decide what an unclaimed dimension should be like, because
     * that is a gameplay judgement and this module has no gameplay in it (ADR-0011). A consumer
     * substitutes its own default &mdash; and for anything oxygen-shaped that default had better
     * be breathable, since a mod that suffocates people in dimensions it was never told about is
     * not a mod anyone should install.
     */
    public static Optional<WorldEnvironment> query(ResourceKey<Level> dimension) {
        return WorldEnvironments.get().resolve(dimension);
    }

    /**
     * Ids of every registered source, in resolution order.
     *
     * <p>For diagnostics. This module deliberately ships no command to print them &mdash; a
     * command is behaviour, and Tier 0 holds contracts rather than behaviour &mdash; so a
     * consumer that wants a "why is this dimension airless" readout builds it from here.
     *
     * @return an immutable list, empty before mod loading completes
     */
    public static List<ResourceLocation> sources() {
        return WorldEnvironments.get().ids();
    }
}
