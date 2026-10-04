package com.ascension.worlds.internal;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Which planets' gateway portals are currently active, network-wide.
 *
 * <p>Shared, not per-player &mdash; a physical structure anyone can walk up to is team/server
 * state, not one more thing tracked per player the way {@link PlanetArrivalMemory} is. Attached
 * to the Overworld's own {@code Level} via {@link SpaceAttachments#GATEWAY_NETWORK_STATE}, the
 * same "attach to a {@code Level} rather than build separate {@code SavedData} plumbing" trick
 * {@link PlanetWeatherState} already uses &mdash; Earth is simply the one dimension every server
 * is guaranteed to have, not a planet this state is conceptually about.
 *
 * <p>Keyed by a planet's surface dimension id, the same identity {@link PlanetArrivalMemory} and
 * {@code WorldsContent.GATEWAY_KEY_FOR_PLANET} already key their own state by &mdash; nothing here
 * needs the {@code Planet} registry itself, only the id.
 *
 * <p>Owned by {@code ascension-worlds} for now rather than a hypothetical
 * {@code ascension-progression} module, per ADR-0011: Tier 1 modules do not reach for a module
 * that does not exist yet. Moving this later is a mechanical relocation, not a design change.
 */
public final class GatewayNetworkState {

    public static final Codec<GatewayNetworkState> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(list -> new GatewayNetworkState(new HashSet<>(list)),
                    state -> List.copyOf(state.activePlanets));

    private final Set<ResourceLocation> activePlanets;

    public GatewayNetworkState() {
        this(new HashSet<>());
    }

    private GatewayNetworkState(Set<ResourceLocation> activePlanets) {
        this.activePlanets = new HashSet<>(activePlanets);
    }

    public boolean isActive(ResourceLocation planetDimension) {
        return activePlanets.contains(planetDimension);
    }

    /** @return true if this planet's gateway was not already active. */
    public boolean activate(ResourceLocation planetDimension) {
        return activePlanets.add(planetDimension);
    }

    /**
     * Whether a portal on {@code from} can send a player to {@code to}: both ends active, and not
     * the same planet. The one rule both the destination list and the travel command apply.
     */
    public boolean canTravel(ResourceLocation from, ResourceLocation to) {
        return !from.equals(to) && isActive(from) && isActive(to);
    }

    public Set<ResourceLocation> activePlanets() {
        return Set.copyOf(activePlanets);
    }
}
