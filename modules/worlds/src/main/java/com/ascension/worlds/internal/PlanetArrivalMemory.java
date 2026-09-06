package com.ascension.worlds.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Where a player last ascended into space from, per planet, so returning to one lands them back
 * near that point instead of at a single fixed spot every time.
 *
 * <p><strong>The ascent point, not wherever they last happened to stand.</strong> An earlier
 * version of this recorded whatever ground position a player was last standing on, polled once a
 * second &mdash; Sanchit's correction: that breaks the moment more than one player is involved.
 * Several players riding the same ship up together each stand somewhere slightly different on it
 * (a block or two apart), and none of those positions has anything to do with each other once
 * that ship is gone. The moment they actually crossed into space together, though, <em>is</em> a
 * shared, meaningful anchor &mdash; it is where the ship was. Recording each player's own position
 * at that single moment (see {@code SpaceMechanics.checkAscent}) keeps them clustered the same way
 * they were clustered on departure, with no need for this module to know anything about ships,
 * Create Aeronautics, or any other vehicle system at all.
 *
 * <p>Keyed by the surface dimension's own id, not the planet's id &mdash; recording happens in
 * {@link SpaceMechanics} purely by watching which dimension a player is ascending from, with no
 * registry lookup needed at read time beyond the one descent already has to do anyway. A recorded
 * position for a dimension nothing currently calls a planet's surface is simply never read back
 * &mdash; harmless, not a bug.
 *
 * <p>Serialised, unlike {@link SpaceTravelState}: this is not a throwaway UI marker but the one
 * piece of state that makes "go back" mean something across a logout or a server restart. Per
 * ADR-0007 rule 1, this stores a {@link BlockPos} and a {@link ResourceLocation}, never a
 * {@code Level}, {@code Player}, or {@code Entity} reference.
 *
 * <p><strong>Landing is resolved near this point, not exactly on it</strong> &mdash; multiple
 * players returning to positions a block or two apart is exactly the coordinate-collision risk
 * Sanchit flagged, on top of ordinary terrain drift (built over, dug out, flooded since they
 * left). See {@code SpaceMechanics.findSafeLandingSpot}.
 */
public final class PlanetArrivalMemory {

    public static final Codec<PlanetArrivalMemory> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, DeparturePoint.CODEC)
                    .xmap(PlanetArrivalMemory::new, memory -> memory.lastAscentFrom);

    private final Map<ResourceLocation, DeparturePoint> lastAscentFrom;

    public PlanetArrivalMemory() {
        this(new HashMap<>());
    }

    /**
     * Defensively copies rather than storing {@code lastAscentFrom} directly &mdash; the crash
     * this fixed. {@code Codec.unboundedMap}'s decode side hands back an immutable map (Guava's
     * {@code ImmutableMap}, in this Mojang codec implementation), which stayed invisible for a
     * brand-new player attachment (built via the no-arg constructor's own fresh {@code HashMap})
     * but threw {@code UnsupportedOperationException} out of {@link #recordAscent} the first time
     * this class was reconstructed by decoding previously-saved data &mdash; i.e. the first ascent
     * after any save/reload cycle, not the first ascent ever. Copying here means every instance is
     * mutable regardless of which constructor path built it, so nothing downstream needs to know
     * or care what the codec's own map implementation happens to be.
     */
    private PlanetArrivalMemory(Map<ResourceLocation, DeparturePoint> lastAscentFrom) {
        this.lastAscentFrom = new HashMap<>(lastAscentFrom);
    }

    /** Where the player last ascended from in the dimension {@code dimensionId}, or {@code null}. */
    public DeparturePoint lastAscentFrom(ResourceLocation dimensionId) {
        return lastAscentFrom.get(dimensionId);
    }

    public void recordAscent(ResourceLocation dimensionId, BlockPos pos, float yaw) {
        lastAscentFrom.put(dimensionId, new DeparturePoint(pos, yaw));
    }

    /** A remembered ascent point: where, and which way the player was facing, when they left. */
    public record DeparturePoint(BlockPos pos, float yaw) {
        public static final Codec<DeparturePoint> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(DeparturePoint::pos),
                        Codec.FLOAT.fieldOf("yaw").forGetter(DeparturePoint::yaw))
                .apply(instance, DeparturePoint::new));
    }
}
