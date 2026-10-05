package com.ascension.worlds.internal;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Which planets' gateway portals are currently active, network-wide, and where each one is.
 *
 * <p>Shared, not per-player &mdash; a physical structure anyone can walk up to is team/server
 * state, not one more thing tracked per player the way {@link PlanetArrivalMemory} is. Attached
 * to the Overworld's own {@code Level} via {@link SpaceAttachments#GATEWAY_NETWORK_STATE}, the
 * same "attach to a {@code Level} rather than build separate {@code SavedData} plumbing" trick
 * {@link PlanetWeatherState} already uses &mdash; Earth is simply the one dimension every server
 * is guaranteed to have, not a planet this state is conceptually about.
 *
 * <p>Keyed by a planet's surface dimension id, the same identity {@link PlanetArrivalMemory} and
 * {@code WorldsContent.GATEWAY_KEY_FOR_PLANET} already key their own state by.
 *
 * <p><strong>Where the portal is</strong> is recorded at activation, from the block actually
 * clicked, so gateway travel lands a player at the destination's portal rather than at the
 * planet's generic arrival point. Taken from the click rather than from worldgen data so it is
 * right for any portal block, generated or placed. A planet activated before positions were
 * tracked has none; travel there falls back to the ordinary landing.
 *
 * <p>Owned by {@code ascension-worlds} for now rather than a hypothetical
 * {@code ascension-progression} module, per ADR-0011: Tier 1 modules do not reach for a module
 * that does not exist yet. Moving this later is a mechanical relocation, not a design change.
 */
public final class GatewayNetworkState {

    private record Entry(ResourceLocation planet, Optional<BlockPos> portal) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        ResourceLocation.CODEC.fieldOf("planet").forGetter(Entry::planet),
                        BlockPos.CODEC.optionalFieldOf("portal").forGetter(Entry::portal))
                .apply(instance, Entry::new));
    }

    /**
     * Current format first; the original bare list of planet ids (before portal positions were
     * tracked) still loads, with no positions.
     */
    public static final Codec<GatewayNetworkState> CODEC = Codec.either(
                    Entry.CODEC.listOf(),
                    ResourceLocation.CODEC.listOf())
            .xmap(
                    either -> either.map(GatewayNetworkState::fromEntries, GatewayNetworkState::fromPlanetIds),
                    state -> Either.left(state.entries()));

    private final Map<ResourceLocation, BlockPos> portals = new HashMap<>();
    private final Set<ResourceLocation> activePlanets = new HashSet<>();

    public GatewayNetworkState() {
    }

    private static GatewayNetworkState fromEntries(List<Entry> entries) {
        GatewayNetworkState state = new GatewayNetworkState();
        for (Entry entry : entries) {
            state.activePlanets.add(entry.planet());
            entry.portal().ifPresent(pos -> state.portals.put(entry.planet(), pos));
        }
        return state;
    }

    private static GatewayNetworkState fromPlanetIds(List<ResourceLocation> planets) {
        GatewayNetworkState state = new GatewayNetworkState();
        state.activePlanets.addAll(planets);
        return state;
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (ResourceLocation planet : activePlanets) {
            entries.add(new Entry(planet, Optional.ofNullable(portals.get(planet))));
        }
        return entries;
    }

    public boolean isActive(ResourceLocation planetDimension) {
        return activePlanets.contains(planetDimension);
    }

    /**
     * Marks this planet's portal active, remembering where it is. Re-activating updates the
     * position, so the most recently used portal on a planet is the one travel lands at.
     *
     * @return true if this planet's gateway was not already active.
     */
    public boolean activate(ResourceLocation planetDimension, BlockPos portal) {
        portals.put(planetDimension, portal.immutable());
        return activePlanets.add(planetDimension);
    }

    /** Where this planet's active portal is, or null if it is inactive or predates tracking. */
    @Nullable
    public BlockPos portalAt(ResourceLocation planetDimension) {
        return portals.get(planetDimension);
    }

    /**
     * Whether a portal on {@code from} can send a player to {@code to}: both ends active, and not
     * the same planet. The one rule both the destination screen and the travel command apply.
     */
    public boolean canTravel(ResourceLocation from, ResourceLocation to) {
        return !from.equals(to) && isActive(from) && isActive(to);
    }

    public Set<ResourceLocation> activePlanets() {
        return Set.copyOf(activePlanets);
    }
}
