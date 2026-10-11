package com.ascension.worlds.internal;

import com.ascension.worlds.internal.gateway.GatewayFrame;
import com.ascension.worlds.internal.worldgen.FixedPositionStructurePlacement;
import com.ascension.worlds.internal.worldgen.GatewayFramePiece;
import com.ascension.worlds.internal.worldgen.GatewayFrameStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.Nullable;

/**
 * Operator-only shortcuts for testing gateways without the playthrough in front of them
 * (Sanchit's ask, 2026-10-06): jump straight to a planet's gateway, and drop a complete frame in
 * front of yourself. Both go through the same code real play does &mdash; {@code goto} lands with
 * {@link SpaceMechanics#travelViaGateway}, {@code build} writes with {@link GatewayFrames#write}
 * &mdash; so they exercise the real paths rather than a debug-only copy of them.
 */
final class GatewayDebug {

    private GatewayDebug() {
    }

    /**
     * Teleports to the front of {@code surface}'s gateway: the attuned one if the network has one,
     * otherwise the pre-generated one (generating its chunk if nobody has been there yet). Returns
     * null on success, or why it could not.
     */
    @Nullable
    static Component goTo(ServerPlayer player, ResourceLocation surface) {
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, surface));
        if (level == null) {
            return Component.literal(surface + " is not a dimension on this server.");
        }
        BlockPos arrival = player.server.overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE).portalAt(surface);
        if (arrival == null) {
            arrival = pregeneratedArrival(level);
        }
        if (arrival == null) {
            return Component.literal("No gateway is attuned on " + surface + ", and none is pre-generated there.");
        }
        if (!SpaceMechanics.travelViaGateway(player, surface, arrival)) {
            return Component.literal(surface + " is not a planet surface.");
        }
        return null;
    }

    /**
     * Where to stand in front of {@code level}'s pre-generated gateway, if any: a
     * {@code fixed_position} placement or a seeking ruin's search says which chunk, and that
     * chunk's structure start says exactly where the frame was laid out.
     */
    @Nullable
    private static BlockPos pregeneratedArrival(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        for (Holder.Reference<Structure> holder : level.registryAccess().registryOrThrow(Registries.STRUCTURE).holders().toList()) {
            if (!(holder.value() instanceof GatewayFrameStructure gate)) {
                continue;
            }
            for (StructurePlacement placement : level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder)) {
                ChunkPos site = null;
                if (gate.seeks()) {
                    site = gate.siteChunk(generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                            level, level.getSeed()).orElse(null);
                } else if (placement instanceof FixedPositionStructurePlacement fixed) {
                    Vec3i at = fixed.locateAt();
                    site = new ChunkPos(SectionPos.blockToSectionCoord(at.getX()), SectionPos.blockToSectionCoord(at.getZ()));
                }
                if (site == null) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(site.x, site.z);
                BlockPos arrival = arrivalFrom(level, chunk.getStartForStructure(holder.value()));
                if (arrival != null) {
                    return arrival;
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos arrivalFrom(ServerLevel level, @Nullable StructureStart start) {
        if (start == null || !start.isValid()) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof GatewayFramePiece framePiece) {
                Optional<GatewayFrame> frame = GatewayFrames.get(level.registryAccess(), framePiece.frame());
                if (frame.isPresent()) {
                    int[] spot = framePiece.placement(frame.get()).arrivalSpot();
                    return new BlockPos(spot[0], spot[1], spot[2]);
                }
            }
        }
        return null;
    }

    /**
     * Writes a complete frame two blocks in front of the player, its opening facing them &mdash;
     * unattuned, or attuned and lit (and registered with the network) if {@code lit}. Returns null
     * on success, or why it could not.
     */
    @Nullable
    static Component build(ServerPlayer player, boolean lit) {
        ServerLevel level = player.serverLevel();
        Optional<GatewayFrame> loaded = GatewayFrames.get(level.registryAccess());
        if (loaded.isEmpty()) {
            return Component.literal("No gateway frame is loaded (" + GatewayFrames.DEFAULT.location() + ").");
        }
        GatewayFrame frame = loaded.get();
        Direction looking = player.getDirection();
        GatewayFrame.Facing facing = GatewayFrame.Facing.valueOf(looking.getOpposite().name());

        BlockPos arrival = player.blockPosition().relative(looking, 2);
        GatewayFrame.Placement placement = frame.placeArrivalAt(arrival.getX(), arrival.getY(), arrival.getZ(), facing);

        GatewayFrames.write(level, frame, placement, lit, null);
        if (lit) {
            GatewayPortalActivation.register(level, placement);
        }
        return null;
    }

    /**
     * Lights or puts out one gate in the network by id, with no frame anywhere, then tells every
     * client and relights the glyphs ({@link GateProgress#networkChanged}). Any id will do, real
     * planet or not, which is how the mark and the pylon are stepped through all seven stages while
     * only two worlds exist. Returns null on success.
     */
    @Nullable
    static Component setLit(MinecraftServer server, ResourceLocation planet, boolean lit) {
        GatewayNetworkState network = server.overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
        boolean changed = lit ? network.activateWithoutPortal(planet) : network.deactivate(planet);
        if (!changed) {
            return Component.literal(planet + " is already " + (lit ? "lit." : "dark."));
        }
        GateProgress.networkChanged(server);
        return null;
    }
}
