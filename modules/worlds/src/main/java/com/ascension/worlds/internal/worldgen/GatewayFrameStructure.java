package com.ascension.worlds.internal.worldgen;

import com.ascension.worlds.internal.GatewayFrames;
import com.ascension.worlds.internal.RingSearch;
import com.ascension.worlds.internal.WorldsContent;
import com.ascension.worlds.internal.gateway.GatewayFrame;
import com.ascension.worlds.internal.gateway.GatewaySite;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A pre-generated gateway: the {@link GatewayFrame} named by {@code frame}, inside the optional
 * {@link GatewaySite} named by {@code site}, built dormant, front facing south. Earth's is
 * player-built.
 *
 * <p>Two ways to say where:
 * <ul>
 *   <li><strong>At its placement chunk</strong> (no {@code seek}): the Moon's, pinned by
 *       {@code ascension_worlds:fixed_position}.</li>
 *   <li><strong>Seeking</strong> ({@code seek}): one per world, in the chunk nearest
 *       {@code seek.near} whose middle column is in this structure's own {@code biomes} and has
 *       solid ground on top, not a lava sea or a lake. The Nether's ruin stands in the warped forest
 *       nearest the origin this way, wherever the seed put that forest. Its structure set must offer
 *       every chunk ({@code random_spread}, spacing 1), and every chunk but the one found makes
 *       nothing; vanilla's {@code /locate} finds it.</li>
 * </ul>
 *
 * <p>Centred on the chunk's middle block and standing on the surface there. A frame up to the
 * contract's 15&times;9 footprint can cross chunk borders; {@link GatewayFramePiece} writes it one
 * chunk at a time, as every multi-chunk piece must.
 */
public final class GatewayFrameStructure extends Structure {

    public static final MapCodec<GatewayFrameStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    settingsCodec(instance),
                    ResourceKey.codec(GatewayFrames.REGISTRY).fieldOf("frame").forGetter(s -> s.frame),
                    ResourceKey.codec(GatewayFrames.SITE_REGISTRY).optionalFieldOf("site").forGetter(s -> s.site),
                    Seek.CODEC.optionalFieldOf("seek").forGetter(s -> s.seek))
            .apply(instance, GatewayFrameStructure::new));

    /** Pre-generated frames face south: arrivals and the {@code goto} debug command rely on it. */
    static final GatewayFrame.Facing FACING = GatewayFrame.Facing.SOUTH;

    private final ResourceKey<GatewayFrame> frame;
    private final Optional<ResourceKey<GatewaySite>> site;
    private final Optional<Seek> seek;

    /**
     * The chunk a seeking ruin settled on, for the one world seed it was worked out for. A seed and
     * a chunk position, never a level (ADR-0007); it lives as long as this structure, which is a
     * datapack registry entry and goes when the server does. Racing worldgen threads can each work
     * it out once, and get the same answer.
     */
    private volatile Found found;

    private record Found(long seed, Optional<ChunkPos> chunk) {
    }

    /**
     * Where a seeking ruin looks: out from {@code near}, at most {@code radius} blocks.
     *
     * <p>One world per seeking structure: the answer is remembered by seed, which every dimension
     * of a world shares, so its {@code biomes} must belong to one dimension only.
     */
    public record Seek(int nearX, int nearZ, int radius) {

        private static final Codec<List<Integer>> NEAR_CODEC = Codec.INT.listOf().validate(list ->
                list.size() == 2
                        ? DataResult.success(list)
                        : DataResult.error(() -> "seek.near must be [x, z], got " + list.size() + " values"));

        public static final Codec<Seek> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        NEAR_CODEC.optionalFieldOf("near", List.of(0, 0)).forGetter(s -> List.of(s.nearX, s.nearZ)),
                        Codec.intRange(16, 8192).optionalFieldOf("radius", 1024).forGetter(Seek::radius))
                .apply(instance, (near, radius) -> new Seek(near.get(0), near.get(1), radius)));
    }

    public GatewayFrameStructure(StructureSettings settings, ResourceKey<GatewayFrame> frame,
                                 Optional<ResourceKey<GatewaySite>> site, Optional<Seek> seek) {
        super(settings);
        this.frame = frame;
        this.site = site;
        this.seek = seek;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        if (seek.isPresent() && !siteChunk(context.chunkGenerator(), context.biomeSource(), context.randomState(),
                context.heightAccessor(), context.seed()).filter(chunkPos::equals).isPresent()) {
            return Optional.empty();
        }
        Optional<GatewayFrame> loaded = GatewayFrames.get(context.registryAccess(), frame);
        if (loaded.isEmpty()) {
            return Optional.empty();
        }
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

        GatewayFrame.Placement centred = loaded.get().placeCentredAt(x, y, z, FACING);
        Optional<GatewaySite> loadedSite = site.flatMap(key -> GatewayFrames.getSite(context.registryAccess(), key));
        BlockPos stub = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(stub, builder ->
                builder.addPiece(new GatewayFramePiece(frame, loaded.get(), site, loadedSite, centred))));
    }

    /** Whether this ruin seeks its ground rather than standing at its placement chunk. */
    public boolean seeks() {
        return seek.isPresent();
    }

    /**
     * The one chunk a seeking ruin stands in, in the world these generator parts belong to, or
     * empty if nothing within its radius qualifies (then it is not built at all). Worked out once
     * per seed and remembered; see {@link #found}.
     */
    public Optional<ChunkPos> siteChunk(ChunkGenerator generator, BiomeSource biomeSource, RandomState randomState,
                                        LevelHeightAccessor heights, long seed) {
        Found remembered = found;
        if (remembered != null && remembered.seed() == seed) {
            return remembered.chunk();
        }
        Optional<ChunkPos> chunk = seek.flatMap(s -> search(s, generator, biomeSource, randomState, heights));
        found = new Found(seed, chunk);
        return chunk;
    }

    /**
     * Rings of chunks out from {@code near}, nearest first; the first whose middle column is in
     * this structure's biomes with solid ground on top wins. The biome is tried first at sea level,
     * which is cheap, and only a column that passes has its height worked out.
     */
    private Optional<ChunkPos> search(Seek seek, ChunkGenerator generator, BiomeSource biomeSource,
                                      RandomState randomState, LevelHeightAccessor heights) {
        int centreX = SectionPos.blockToSectionCoord(seek.nearX());
        int centreZ = SectionPos.blockToSectionCoord(seek.nearZ());
        int seaLevel = generator.getSeaLevel();
        return RingSearch.first(seek.radius() >> 4, offset -> {
            int x = SectionPos.sectionToBlockCoord(centreX + offset.dx(), 8);
            int z = SectionPos.sectionToBlockCoord(centreZ + offset.dz(), 8);
            if (!inBiomes(biomeSource, randomState, x, seaLevel, z)) {
                return false;
            }
            OptionalInt ground = DryGroundStructure.dryGroundAt(generator, heights, randomState, x, z);
            return ground.isPresent() && inBiomes(biomeSource, randomState, x, ground.getAsInt(), z);
        }).map(offset -> new ChunkPos(centreX + offset.dx(), centreZ + offset.dz()));
    }

    private boolean inBiomes(BiomeSource biomeSource, RandomState randomState, int x, int y, int z) {
        return biomes().contains(biomeSource.getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler()));
    }

    @Override
    public StructureType<?> type() {
        return WorldsContent.GATEWAY_FRAME_STRUCTURE_TYPE.get();
    }
}
