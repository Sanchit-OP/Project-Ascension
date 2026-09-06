package com.ascension.worlds.internal.terrain;

import com.ascension.worlds.internal.WorldsContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Where a large crater starts, and how big this particular one rolls.
 *
 * <p>A {@code Structure} rather than a {@code Feature} &mdash; see {@link CraterPiece}'s javadoc
 * for why. This class only decides a spawn point (jittered a little within its chunk, the same
 * "no two instances identical" reasoning {@link CraterConfiguration} already used for radius) and
 * hands the actual shape to one {@link CraterPiece}. Small craters stay a {@link CraterFeature}:
 * their radius (5&ndash;10) never gets close to the one-chunk write limit a {@code Feature} is
 * bound by, so there's nothing here for them to gain.
 */
public final class CraterStructure extends Structure {

    public static final MapCodec<CraterStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    settingsCodec(instance),
                    IntProvider.codec(1, 60).fieldOf("radius").forGetter(s -> s.radius),
                    Codec.intRange(0, 48).fieldOf("floor_depth").forGetter(s -> s.floorDepth),
                    Codec.intRange(0, 12).fieldOf("rim_height").forGetter(s -> s.rimHeight))
            .apply(instance, CraterStructure::new));

    private final IntProvider radius;
    private final int floorDepth;
    private final int rimHeight;

    public CraterStructure(Structure.StructureSettings settings, IntProvider radius, int floorDepth, int rimHeight) {
        super(settings);
        this.radius = radius;
        this.floorDepth = floorDepth;
        this.rimHeight = rimHeight;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        RandomSource random = context.random();
        // A few blocks of jitter so every large crater isn't dead-centre in its chunk -- purely
        // cosmetic variety now; unlike the old Feature version, placement here never has to be
        // biased toward the chunk centre for safety, because CraterPiece.postProcess is safe to
        // write from at any origin.
        int x = chunkPos.getMiddleBlockX() + random.nextInt(9) - 4;
        int z = chunkPos.getMiddleBlockZ() + random.nextInt(9) - 4;
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

        int sampledRadius = this.radius.sample(random);
        int floorDepth = this.floorDepth;
        int rimHeight = this.rimHeight;
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight() - 1;

        return Optional.of(new Structure.GenerationStub(new BlockPos(x, y, z), builder ->
                builder.addPiece(new CraterPiece(x, z, sampledRadius, floorDepth, rimHeight, minY, maxY))));
    }

    @Override
    public StructureType<?> type() {
        return WorldsContent.CRATER_STRUCTURE_TYPE.get();
    }
}
