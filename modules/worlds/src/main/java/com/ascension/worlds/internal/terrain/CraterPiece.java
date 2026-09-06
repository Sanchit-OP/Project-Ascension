package com.ascension.worlds.internal.terrain;

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The bowl-plus-rim shape itself, one piece per crater.
 *
 * <p>This is {@link CraterFeature}'s carve/rim algorithm, moved here for one reason: a
 * {@code Feature} can only safely write blocks within one chunk of wherever it started (Minecraft
 * hard-codes that limit for the {@code FEATURES} generation step), and a large crater's rim
 * reaches up to ~52 blocks from its centre &mdash; two to three chunks further than a feature is
 * allowed to touch. Blocks past that limit were being silently dropped (logged as "setBlock in a
 * far chunk"), which is what produced clipped-looking craters. A {@link StructurePiece} doesn't
 * have that ceiling: the game calls {@link #postProcess} once for <em>every</em> chunk the piece's
 * bounding box overlaps, handing over that chunk's own safe write area each time, so a crater
 * spanning several chunks gets written completely, one chunk-sized slice per call, instead of
 * once from a single origin.
 *
 * <p>The carve/rim math itself is unchanged from the feature version &mdash; only how the writes
 * are scoped to a safe area is different.
 */
public final class CraterPiece extends StructurePiece {

    private final int originX;
    private final int originZ;
    private final int radius;
    private final int floorDepth;
    private final int rimHeight;

    public CraterPiece(int originX, int originZ, int radius, int floorDepth, int rimHeight, int minY, int maxY) {
        super(WorldsContent.CRATER_PIECE.get(), 0, computeBoundingBox(originX, originZ, radius, minY, maxY));
        this.originX = originX;
        this.originZ = originZ;
        this.radius = radius;
        this.floorDepth = floorDepth;
        this.rimHeight = rimHeight;
        this.setOrientation(null);
    }

    public CraterPiece(CompoundTag tag) {
        super(WorldsContent.CRATER_PIECE.get(), tag);
        this.originX = tag.getInt("OriginX");
        this.originZ = tag.getInt("OriginZ");
        this.radius = tag.getInt("Radius");
        this.floorDepth = tag.getInt("FloorDepth");
        this.rimHeight = tag.getInt("RimHeight");
        this.setOrientation(null);
    }

    private static BoundingBox computeBoundingBox(int originX, int originZ, int radius, int minY, int maxY) {
        int searchRadius = radius + Math.max(1, radius / 6);
        return new BoundingBox(originX - searchRadius, minY, originZ - searchRadius,
                originX + searchRadius, maxY, originZ + searchRadius);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OriginX", this.originX);
        tag.putInt("OriginZ", this.originZ);
        tag.putInt("Radius", this.radius);
        tag.putInt("FloorDepth", this.floorDepth);
        tag.putInt("RimHeight", this.rimHeight);
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
        // rimBand mirrors CraterFeature: a sixth of the radius, wide enough to read as a rim
        // rather than a wall, narrow enough not to blur two nearby craters into one shape.
        int rimBand = Math.max(1, this.radius / 6);
        int searchRadius = this.radius + rimBand;

        // Only scan the slice of the crater that falls inside THIS chunk -- postProcess runs once
        // per chunk the piece's bounding box overlaps, so re-scanning the full crater footprint
        // every time would be wasted work (and the far corners would just get clipped by
        // ensureCanWrite again, right back to the original bug).
        int minX = Math.max(this.originX - searchRadius, chunkPos.getMinBlockX());
        int maxX = Math.min(this.originX + searchRadius, chunkPos.getMaxBlockX());
        int minZ = Math.max(this.originZ - searchRadius, chunkPos.getMinBlockZ());
        int maxZ = Math.min(this.originZ + searchRadius, chunkPos.getMaxBlockZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        BlockState regolith = WorldsContent.MOON_REGOLITH.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - this.originX;
                int dz = z - this.originZ;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > searchRadius) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                if (distance <= this.radius) {
                    double falloff = 1.0 - (distance * distance) / (this.radius * (double) this.radius);
                    int depthHere = (int) Math.round(this.floorDepth * falloff);
                    for (int y = surfaceY; y > surfaceY - depthHere; y--) {
                        cursor.set(x, y, z);
                        if (level.getBlockState(cursor).isAir()) {
                            break;
                        }
                        if (belongsToOtherStructure(structureManager, cursor)) {
                            // Something else already claims this block -- stop carving down
                            // through it rather than hollowing out (or undermining) whatever
                            // that structure turns out to be.
                            break;
                        }
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                    }
                } else {
                    double bandPosition = (distance - this.radius) / rimBand;
                    int riseHere = (int) Math.round(this.rimHeight * (1.0 - bandPosition));
                    for (int i = 1; i <= riseHere; i++) {
                        cursor.set(x, surfaceY + i, z);
                        if (belongsToOtherStructure(structureManager, cursor)) {
                            break;
                        }
                        level.setBlock(cursor, regolith, 3);
                    }
                }
            }
        }
    }

    /**
     * True if {@code pos} already belongs to some other generated structure's piece.
     *
     * <p>Craters generate after most other worldgen (see the {@code LOCAL_MODIFICATIONS}/
     * {@code SURFACE_STRUCTURES} step ordering discussion elsewhere in this package), so nothing
     * stops a bowl or rim from carving straight through whatever a later-added structure places
     * nearby. Rather than teaching every future structure about craters (a coupling that would
     * have to be remembered and re-added each time a new structure type shows up), craters check
     * outward instead: {@link StructureManager#getStructureWithPieceAt} answers "is any generated
     * structure's piece sitting at this exact block" regardless of what that structure is, so this
     * stays correct with zero changes here when new structures are added later.
     *
     * <p>Excludes {@link CraterStructure} itself -- otherwise a crater's own piece (already
     * registered by the time {@link #postProcess} runs) would match every position it is about to
     * write, and the crater would carve nothing at all.
     */
    private static boolean belongsToOtherStructure(StructureManager structureManager, BlockPos pos) {
        return structureManager.getStructureWithPieceAt(pos, holder -> !(holder.value() instanceof CraterStructure)).isValid();
    }
}
