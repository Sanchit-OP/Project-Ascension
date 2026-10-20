package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Any structure, refused where its start would stand on a lava sea or a lake: the Nether's colony
 * ruins and the Sealers' carving (design's handover, {@code nether-art-format.md}).
 *
 * <p>Vanilla projects a jigsaw start onto {@code WORLD_SURFACE_WG}, which counts fluids, so on the
 * roofless Nether one ruin in twenty-three stood on a lava sea at y 31. This wraps the real
 * structure (named or inline in {@code structure}) and keeps its answer only if the ground at the
 * start and {@code check_radius} blocks out in each direction has nothing fluid on top: the two
 * worldgen heightmaps agree there. The same test the gate ruin's seek uses.
 *
 * <p>{@code min_ground}, if given, also refuses a start whose ground is lower than that: the End's islands hang over a
 * void where both heightmaps agree at the world's floor, so only a height says there is rock.
 *
 * <p>The wrapper's own settings ({@code biomes}, {@code step}, {@code terrain_adaptation}) are the
 * ones that apply, since the start is this structure's; the inner one's biomes must also admit the
 * spot, as it checks its own.
 */
public final class DryGroundStructure extends Structure {

    public static final MapCodec<DryGroundStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    settingsCodec(instance),
                    Structure.CODEC.fieldOf("structure").forGetter(s -> s.inner),
                    Codec.intRange(0, 32).optionalFieldOf("check_radius", 8).forGetter(s -> s.checkRadius),
                    Codec.INT.optionalFieldOf("min_ground").forGetter(s -> s.minGround))
            .apply(instance, DryGroundStructure::new));

    private final Holder<Structure> inner;
    private final int checkRadius;
    private final Optional<Integer> minGround;

    public DryGroundStructure(StructureSettings settings, Holder<Structure> inner, int checkRadius,
                              Optional<Integer> minGround) {
        super(settings);
        this.inner = inner;
        this.checkRadius = checkRadius;
        this.minGround = minGround;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return inner.value().findValidGenerationPoint(context).filter(stub -> dryAround(context, stub.position()));
    }

    private boolean dryAround(GenerationContext context, BlockPos at) {
        int[][] offsets = {{0, 0}, {checkRadius, 0}, {-checkRadius, 0}, {0, checkRadius}, {0, -checkRadius}};
        for (int[] offset : offsets) {
            OptionalInt ground = dryGroundAt(context.chunkGenerator(), context.heightAccessor(), context.randomState(),
                    at.getX() + offset[0], at.getZ() + offset[1]);
            // min_ground: over the End's void the two heightmaps agree at the world's floor, so "dry" is not
            // enough there; the ground must reach this high.
            if (ground.isEmpty() || minGround.isPresent() && ground.getAsInt() < minGround.get()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The top of the ground in this column, if nothing fluid lies over it: the worldgen surface
     * that counts fluids and the one that does not agree. Empty over a lava sea, a lake or the
     * void. Shared with the seeking gate ruin ({@link GatewayFrameStructure}).
     */
    static OptionalInt dryGroundAt(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState,
                                   int x, int z) {
        int top = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
        int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
        return top == ground && top >= heights.getMinBuildHeight() ? OptionalInt.of(top) : OptionalInt.empty();
    }

    @Override
    public StructureType<?> type() {
        return WorldsContent.DRY_GROUND_STRUCTURE_TYPE.get();
    }
}
