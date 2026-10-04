package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.WorldsContent;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Where one planet's copy of the gateway portal sits: dead-centre of its placement chunk, resting
 * on the real surface.
 *
 * <p>One structure type, several structure JSON instances &mdash;
 * {@code worldgen/structure/gateway_portal_earth.json} and {@code gateway_portal_moon.json} both
 * point at this same type with their own {@code biomes} restriction and their own
 * {@code ascension_worlds:fixed_position} coordinate. Per ADR-0007 rule 10, a planet's own portal
 * is authoring, not a new class; Planet 3's copy reuses this Java unchanged.
 *
 * <p>A single block is small enough that the one-chunk write limit
 * {@link com.ascension.worlds.internal.terrain.CraterPiece}'s javadoc explains never applies here
 * &mdash; see {@link GatewayPortalPiece}.
 */
public final class GatewayPortalStructure extends Structure {

    public static final MapCodec<GatewayPortalStructure> CODEC = simpleCodec(GatewayPortalStructure::new);

    public GatewayPortalStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        BlockPos pos = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(pos, builder -> builder.addPiece(new GatewayPortalPiece(pos))));
    }

    @Override
    public StructureType<?> type() {
        return WorldsContent.GATEWAY_PORTAL_STRUCTURE_TYPE.get();
    }
}
