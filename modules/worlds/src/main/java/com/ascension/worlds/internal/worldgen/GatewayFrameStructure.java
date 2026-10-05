package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.GatewayFrames;
import com.ascension.worlds.internal.WorldsContent;
import com.ascension.worlds.internal.gateway.GatewayFrame;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A pre-generated gateway: the {@link GatewayFrame} named by {@code frame}, built dormant at its
 * placement chunk, front facing south &mdash; today only the Moon's
 * ({@code worldgen/structure/gateway_frame_moon.json}, pinned by
 * {@code ascension_worlds:fixed_position}). Earth's is player-built.
 *
 * <p>Centred on the chunk's middle block and standing on the surface there. A frame up to the
 * contract's 15&times;9 footprint can cross chunk borders; {@link GatewayFramePiece} writes it one
 * chunk at a time, as every multi-chunk piece must.
 */
public final class GatewayFrameStructure extends Structure {

    public static final MapCodec<GatewayFrameStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    settingsCodec(instance),
                    ResourceKey.codec(GatewayFrames.REGISTRY).fieldOf("frame").forGetter(s -> s.frame))
            .apply(instance, GatewayFrameStructure::new));

    /** Pre-generated frames face south: arrivals and the {@code goto} debug command rely on it. */
    static final GatewayFrame.Facing FACING = GatewayFrame.Facing.SOUTH;

    private final ResourceKey<GatewayFrame> frame;

    public GatewayFrameStructure(StructureSettings settings, ResourceKey<GatewayFrame> frame) {
        super(settings);
        this.frame = frame;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Optional<GatewayFrame> loaded = GatewayFrames.get(context.registryAccess(), frame);
        if (loaded.isEmpty()) {
            return Optional.empty();
        }
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

        GatewayFrame.Placement centred = centredOn(loaded.get(), x, y, z);
        BlockPos stub = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(stub, builder ->
                builder.addPiece(new GatewayFramePiece(frame, loaded.get(), centred))));
    }

    /** The placement whose footprint is centred on {@code (x, z)}, bottom layer at {@code y}. */
    private static GatewayFrame.Placement centredOn(GatewayFrame frame, int x, int y, int z) {
        int maxX = 0;
        int maxZ = 0;
        for (GatewayFrame.Cell cell : frame.cells()) {
            maxX = Math.max(maxX, cell.x());
            maxZ = Math.max(maxZ, cell.z());
        }
        // South-facing: local +x is world +x, local +z (back) is world -z.
        return frame.placeOriginAt(x - maxX / 2, y, z + maxZ / 2, FACING);
    }

    @Override
    public StructureType<?> type() {
        return WorldsContent.GATEWAY_FRAME_STRUCTURE_TYPE.get();
    }
}
