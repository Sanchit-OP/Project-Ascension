package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.GatewayFrames;
import com.ascension.worlds.internal.WorldsContent;
import com.ascension.worlds.internal.gateway.GatewayFrame;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Writes a pre-generated {@link GatewayFrame} into the world: every frame cell, the keystone
 * unattuned, the opening cleared to air (terrain must not block it, or activation would fail),
 * and a foundation under the bottom layer down to solid ground so it never hangs over a dip.
 *
 * <p>Called once per chunk the frame overlaps, each time with that chunk's {@code box}; only cells
 * inside it are written ({@link GatewayFrames#write}'s clip), which is what keeps a frame wider
 * than a chunk inside vanilla's one-chunk write limit (see {@code CraterPiece}).
 */
public final class GatewayFramePiece extends StructurePiece {

    static final int FOUNDATION_DEPTH = 8;

    private final ResourceKey<GatewayFrame> frame;
    private final BlockPos origin;
    private final GatewayFrame.Facing facing;

    GatewayFramePiece(ResourceKey<GatewayFrame> frame, GatewayFrame loaded, GatewayFrame.Placement placement) {
        super(WorldsContent.GATEWAY_FRAME_PIECE.get(), 0, bounds(loaded, placement));
        this.frame = frame;
        int[] at = placement.world(0, 0, 0);
        this.origin = new BlockPos(at[0], at[1], at[2]);
        this.facing = placement.facing();
        this.setOrientation(null);
    }

    public GatewayFramePiece(CompoundTag tag) {
        super(WorldsContent.GATEWAY_FRAME_PIECE.get(), tag);
        this.frame = ResourceKey.create(GatewayFrames.REGISTRY, ResourceLocation.parse(tag.getString("Frame")));
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.facing = GatewayFrame.Facing.valueOf(tag.getString("Facing"));
        this.setOrientation(null);
    }

    private static BoundingBox bounds(GatewayFrame frame, GatewayFrame.Placement placement) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (GatewayFrame.Cell cell : frame.cells()) {
            int[] at = placement.world(cell.x(), cell.y(), cell.z());
            minX = Math.min(minX, at[0]);
            minY = Math.min(minY, at[1]);
            minZ = Math.min(minZ, at[2]);
            maxX = Math.max(maxX, at[0]);
            maxY = Math.max(maxY, at[1]);
            maxZ = Math.max(maxZ, at[2]);
        }
        return new BoundingBox(minX, minY - FOUNDATION_DEPTH, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Frame", frame.location().toString());
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginY", origin.getY());
        tag.putInt("OriginZ", origin.getZ());
        tag.putString("Facing", facing.name());
    }

    /** The frame as it was laid out, for anyone needing to find it again (arrivals, {@code goto}). */
    public GatewayFrame.Placement placement(GatewayFrame loaded) {
        return loaded.placeOriginAt(origin.getX(), origin.getY(), origin.getZ(), facing);
    }

    public ResourceKey<GatewayFrame> frame() {
        return frame;
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
        Optional<GatewayFrame> loaded = GatewayFrames.get(level.registryAccess(), frame);
        if (loaded.isEmpty()) {
            return;
        }
        GatewayFrame gatewayFrame = loaded.get();
        GatewayFrame.Placement placement = placement(gatewayFrame);

        writeFoundation(level, gatewayFrame, placement, box);
        GatewayFrames.write(level, gatewayFrame, placement, false, box);
    }

    /** Under each bottom-layer frame cell, its own block down to the first solid one. */
    private static void writeFoundation(WorldGenLevel level, GatewayFrame frame, GatewayFrame.Placement placement,
                                        BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (GatewayFrame.Cell cell : frame.cells()) {
            if (cell.y() != 0 || cell.symbol() == frame.fieldSymbol() || cell.symbol() == frame.keystoneSymbol()) {
                continue;
            }
            BlockState fill = GatewayFrames.placedState(frame.key().get(cell.symbol()));
            int[] at = placement.world(cell.x(), cell.y(), cell.z());
            for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
                cursor.set(at[0], at[1] - depth, at[2]);
                if (!box.isInside(cursor) || !level.getBlockState(cursor).canBeReplaced()) {
                    break;
                }
                level.setBlock(cursor, fill, Block.UPDATE_CLIENTS);
            }
        }
    }
}
