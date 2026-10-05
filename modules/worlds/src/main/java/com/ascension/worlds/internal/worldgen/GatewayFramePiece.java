package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.GatewayFrames;
import com.ascension.worlds.internal.WorldsContent;
import com.ascension.worlds.internal.gateway.GatewayFrame;
import com.ascension.worlds.internal.gateway.GatewaySite;
import java.util.List;
import java.util.Map;
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
 * Writes a pre-generated gateway into the world: its {@link GatewaySite} decoration if it has one,
 * then the {@link GatewayFrame} itself &mdash; every frame cell, the keystone unattuned, the opening
 * cleared to air (terrain must not block it, or attunement would fail).
 *
 * <p><strong>Foundations.</strong> The lowest layer of the site (or, with no site, of the frame) is
 * extended down through anything replaceable to solid ground, at most {@value #FOUNDATION_DEPTH}
 * blocks, so the structure never hangs over a crater rim or a dip. The design session asked for
 * exactly this for the site (gateway-portal-design.md, "site terrain conforming"). The frame gets
 * the same treatment, which only does anything where nothing solid is under its bottom layer.
 *
 * <p>Called once per chunk the structure overlaps, each time with that chunk's {@code box}; only
 * cells inside it are written, which keeps a structure wider than a chunk inside vanilla's
 * one-chunk write limit (see {@code CraterPiece}). Site cells never overlap frame cells (the design
 * guarantees it), so the order the two are written in does not matter.
 */
public final class GatewayFramePiece extends StructurePiece {

    static final int FOUNDATION_DEPTH = 8;

    private final ResourceKey<GatewayFrame> frame;
    private final Optional<ResourceKey<GatewaySite>> site;
    private final BlockPos origin;
    private final GatewayFrame.Facing facing;

    GatewayFramePiece(ResourceKey<GatewayFrame> frame, GatewayFrame loaded,
                      Optional<ResourceKey<GatewaySite>> site, Optional<GatewaySite> loadedSite,
                      GatewayFrame.Placement placement) {
        super(WorldsContent.GATEWAY_FRAME_PIECE.get(), 0, bounds(loaded, loadedSite, placement));
        this.frame = frame;
        this.site = site;
        int[] at = placement.world(0, 0, 0);
        this.origin = new BlockPos(at[0], at[1], at[2]);
        this.facing = placement.facing();
        this.setOrientation(null);
    }

    public GatewayFramePiece(CompoundTag tag) {
        super(WorldsContent.GATEWAY_FRAME_PIECE.get(), tag);
        this.frame = ResourceKey.create(GatewayFrames.REGISTRY, ResourceLocation.parse(tag.getString("Frame")));
        this.site = tag.contains("Site")
                ? Optional.of(ResourceKey.create(GatewayFrames.SITE_REGISTRY, ResourceLocation.parse(tag.getString("Site"))))
                : Optional.empty();
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.facing = GatewayFrame.Facing.valueOf(tag.getString("Facing"));
        this.setOrientation(null);
    }

    private static BoundingBox bounds(GatewayFrame frame, Optional<GatewaySite> site, GatewayFrame.Placement placement) {
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (GatewayFrame.Cell cell : frame.cells()) {
            include(min, max, placement.world(cell.x(), cell.y(), cell.z()));
        }
        site.ifPresent(s -> s.cells().forEach(cell -> include(min, max, s.world(placement, cell))));
        return new BoundingBox(min[0], min[1] - FOUNDATION_DEPTH, min[2], max[0], max[1], max[2]);
    }

    private static void include(int[] min, int[] max, int[] at) {
        for (int i = 0; i < 3; i++) {
            min[i] = Math.min(min[i], at[i]);
            max[i] = Math.max(max[i], at[i]);
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Frame", frame.location().toString());
        site.ifPresent(key -> tag.putString("Site", key.location().toString()));
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
        Optional<GatewaySite> loadedSite = site.flatMap(key -> GatewayFrames.getSite(level.registryAccess(), key));

        if (loadedSite.isPresent()) {
            writeSite(level, loadedSite.get(), placement, box);
        } else {
            writeFoundation(level, gatewayFrame.cells(), gatewayFrame.key(), placement::world, box);
        }
        GatewayFrames.write(level, gatewayFrame, placement, false, box);
    }

    private static void writeSite(WorldGenLevel level, GatewaySite site, GatewayFrame.Placement placement, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (GatewayFrame.Cell cell : site.cells()) {
            int[] at = site.world(placement, cell);
            if (box.isInside(cursor.set(at[0], at[1], at[2]))) {
                level.setBlock(cursor, GatewayFrames.placedState(site.key().get(cell.symbol())), Block.UPDATE_CLIENTS);
            }
        }
        writeFoundation(level, site.cells(), site.key(), (x, y, z) -> {
            var offset = site.frameOffset();
            return placement.world(x - offset.getX(), y - offset.getY(), z - offset.getZ());
        }, box);
    }

    /** Where a grid's local cell lands in the world. */
    @FunctionalInterface
    private interface ToWorld {
        int[] at(int x, int y, int z);
    }

    /** Under each bottom-layer cell of a grid, that cell's own block down to the first solid one. */
    private static void writeFoundation(WorldGenLevel level, List<GatewayFrame.Cell> cells,
                                        Map<Character, GatewayFrame.Key> key, ToWorld toWorld, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (GatewayFrame.Cell cell : cells) {
            GatewayFrame.Key entry = key.get(cell.symbol());
            if (cell.y() != 0 || entry == null) {
                continue;
            }
            BlockState fill = GatewayFrames.placedState(entry);
            if (fill.is(WorldsContent.GATEWAY_KEYSTONE.get())) {
                continue;
            }
            int[] at = toWorld.at(cell.x(), cell.y(), cell.z());
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
