package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Places one gateway portal block, at the exact surface point {@link GatewayPortalStructure}
 * found. Inert until a player satisfies that planet's own activation requirement &mdash; see
 * {@code GatewayPortalBlock}.
 */
public final class GatewayPortalPiece extends StructurePiece {

    private final BlockPos origin;

    public GatewayPortalPiece(BlockPos origin) {
        super(WorldsContent.GATEWAY_PORTAL_PIECE.get(), 0,
                new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                        origin.getX(), origin.getY(), origin.getZ()));
        this.origin = origin;
        this.setOrientation(null);
    }

    public GatewayPortalPiece(CompoundTag tag) {
        super(WorldsContent.GATEWAY_PORTAL_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pos) {
        if (!box.isInside(this.origin)) {
            return;
        }
        level.setBlock(this.origin, WorldsContent.GATEWAY_PORTAL.get().defaultBlockState(), 3);
    }
}
