package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * A structure_set placement that always resolves to the same chunk, every world, every seed.
 *
 * <p>Neither of vanilla's own two placement types can do this: {@code random_spread} and
 * {@code concentric_rings} both fold the world seed into a hash to decide where an instance
 * lands, which is exactly the point for a stronghold or a village &mdash; findable, not
 * hand-placed. A mandatory boss arena is the opposite: it needs to be at a specific, known spot
 * on one specific planet, reachable the same way for every player on every world this project
 * generates, not wherever a seed's jitter happens to put it.
 *
 * <p>This is the reusable half of sourcing a boss from a third-party mod. Placing it is a
 * two-layer problem &mdash; which chunk is a candidate ({@code structure_set}, this class), and
 * exactly where within that chunk the structure resolves ({@code Structure.findGenerationPoint},
 * which stays the third-party mod's own code untouched). Every future
 * {@code ascension-compat-<bossmod>} jar reuses this same placement type by id
 * ({@code ascension_worlds:fixed_position}) in its own {@code structure_set} override; none of
 * them need new Java for it.
 *
 * <p>Block coordinates in the datapack, not chunk coordinates &mdash; {@code x}/{@code z} are
 * what a planet's design doc and a player's coordinates both already speak in.
 */
public final class FixedPositionStructurePlacement extends StructurePlacement {

    public static final MapCodec<FixedPositionStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            placementCodec(instance)
                    .and(Vec3i.CODEC.fieldOf("locate_at").forGetter(FixedPositionStructurePlacement::locateAt))
                    .apply(instance, FixedPositionStructurePlacement::new));

    private final Vec3i locateAt;
    private final int chunkX;
    private final int chunkZ;

    public FixedPositionStructurePlacement(
            Vec3i locateOffset,
            FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<ExclusionZone> exclusionZone,
            Vec3i locateAt) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
        this.locateAt = locateAt;
        this.chunkX = SectionPos.blockToSectionCoord(locateAt.getX());
        this.chunkZ = SectionPos.blockToSectionCoord(locateAt.getZ());
    }

    private Vec3i locateAt() {
        return this.locateAt;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return chunkX == this.chunkX && chunkZ == this.chunkZ;
    }

    @Override
    public StructurePlacementType<?> type() {
        return WorldsContent.FIXED_POSITION_PLACEMENT_TYPE.get();
    }
}
