package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A gateway portal as players find it: a 3&times;3 obsidian pillar {@value #HEIGHT} blocks tall,
 * with the portal block itself set into the middle of its south face at chest height, one block
 * up. Sanchit's call after the first live test (2026-10-05): a lone block on the surface was
 * effectively unfindable, and a pillar reads from a distance as "something was built here".
 *
 * <p>The south face is a convention gateway travel relies on: arrivals land one block south of
 * the portal block, facing it ({@code SpaceMechanics.travelViaGateway}).
 *
 * <p>Each column also gets an obsidian foundation down to solid ground (at most
 * {@value #FOUNDATION_DEPTH} blocks), so the pillar never hangs over a dip on uneven terrain.
 *
 * <p>Centred on its chunk's middle block ({@link GatewayPortalStructure}), the whole footprint
 * stays inside one chunk, so the one-chunk write limit {@code CraterPiece}'s javadoc explains
 * never comes into play; the {@code box} checks below are belt and braces.
 */
public final class GatewayPortalPiece extends StructurePiece {

    static final int HEIGHT = 10;
    static final int FOUNDATION_DEPTH = 8;

    /** The pillar's base centre: the first air block above the ground, from the heightmap. */
    private final BlockPos origin;

    public GatewayPortalPiece(BlockPos origin) {
        super(WorldsContent.GATEWAY_PORTAL_PIECE.get(), 0,
                new BoundingBox(origin.getX() - 1, origin.getY() - FOUNDATION_DEPTH, origin.getZ() - 1,
                        origin.getX() + 1, origin.getY() + HEIGHT - 1, origin.getZ() + 1));
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
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
                    cursor.set(x, origin.getY() - depth, z);
                    if (!box.isInside(cursor) || !level.getBlockState(cursor).canBeReplaced()) {
                        break;
                    }
                    level.setBlock(cursor, obsidian, 2);
                }

                for (int dy = 0; dy < HEIGHT; dy++) {
                    cursor.set(x, origin.getY() + dy, z);
                    if (box.isInside(cursor)) {
                        level.setBlock(cursor, obsidian, 2);
                    }
                }
            }
        }

        BlockPos core = origin.offset(0, 1, 1);
        if (box.isInside(core)) {
            level.setBlock(core, WorldsContent.GATEWAY_PORTAL.get().defaultBlockState(), 2);
        }
    }
}
