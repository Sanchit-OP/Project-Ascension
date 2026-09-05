package com.ascension.core.api;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Something that knows what a dimension is like to be in.
 *
 * <p>Register one to describe your own dimensions without anything having to know your mod
 * exists. {@code ascension-worlds} registers one backed by its planet data; a third-party
 * dimension mod can register one just as well, and every consumer of
 * {@link WorldEnvironmentRegistry} will honour it.
 *
 * <p><strong>Answer only for dimensions you own.</strong> Return {@link Optional#empty()} for
 * everything else. A source that claims dimensions it knows nothing about will shadow the mod
 * that actually owns them, and the conflict is reported rather than silently resolved.
 *
 * <p>Called on the server thread while resolving. Keep it a lookup: read your own data, do not
 * touch the world.
 */
@FunctionalInterface
public interface WorldEnvironmentSource {

    /**
     * What this dimension is like, if this source knows.
     *
     * @param dimension the dimension being asked about
     * @return its environment, or empty if this source does not own that dimension
     */
    Optional<WorldEnvironment> environmentOf(ResourceKey<Level> dimension);
}
