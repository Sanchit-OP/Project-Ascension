package com.ascension.atmosphere.api;

import com.ascension.atmosphere.internal.ProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Entry point for contributing to, and asking about, the atmosphere.
 *
 * <p>Registration must happen during mod setup. The registry is frozen once the server begins
 * resolving, so that queries never pay to sort and never see a half-built provider list;
 * registering afterwards throws.
 *
 * <p>Every registration takes a stable id. Ids cost the caller nothing and buy two things
 * worth far more: {@code /ascension atmosphere why} can name who claimed a position, and
 * conflicts between providers at equal priority resolve deterministically instead of by
 * whatever order mods happened to load in.
 */
public final class AtmosphereRegistry {

    private AtmosphereRegistry() {
    }

    /**
     * Contribute an atmosphere source.
     *
     * @param id       stable identifier, typically {@code yourmod:what_it_covers}
     * @param provider consulted in descending priority until one claims the position
     * @throws IllegalStateException if setup has already completed
     * @throws IllegalArgumentException if {@code id} is already registered
     */
    public static void register(ResourceLocation id, AtmosphereProvider provider) {
        ProviderRegistry.get().addProvider(id, provider);
    }

    /**
     * Contribute a way of finding a player's oxygen sources.
     *
     * @throws IllegalStateException if setup has already completed
     */
    public static void register(ResourceLocation id, OxygenSourceCollector collector) {
        ProviderRegistry.get().addCollector(id, collector);
    }

    /**
     * Contribute a consumption multiplier.
     *
     * @throws IllegalStateException if setup has already completed
     */
    public static void register(DrainModifier modifier) {
        ProviderRegistry.get().addModifier(modifier);
    }

    /**
     * Contribute extra lung capacity, typically from worn gear.
     *
     * <p>Named rather than overloaded: {@link LungCapacityModifier} and
     * {@link AtmosphereProvider} are both functional interfaces taking an id, so an overload
     * would make every lambda call site ambiguous.
     *
     * @throws IllegalStateException if setup has already completed
     */
    public static void registerLungCapacity(ResourceLocation id, LungCapacityModifier modifier) {
        ProviderRegistry.get().addLungModifier(id, modifier);
    }

    /**
     * The atmosphere at a position.
     *
     * <p>Never null: with no provider claiming the position, the result is
     * {@link Atmosphere#BREATHABLE}. An absent atmosphere system should not suffocate anyone,
     * so the safe default is air.
     */
    public static Atmosphere query(ServerLevel level, Vec3 position) {
        return ProviderRegistry.get().resolve(new AtmosphereContext(level, position));
    }
}
