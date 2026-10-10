package com.ascension.compat.sable.internal;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.util.SableNBTUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moves a ship from one level to another, crew and all, keeping its orientation: the one thing
 * Sable has no call for, and the thing a ship flying from a planet into space needs.
 *
 * <p><strong>Through Sable's own save and load.</strong> A ship whose chunks unload is written out
 * by {@code SubLevelSerializer.toData} and read back by {@code fullyLoad}, so its blocks, block
 * entities and Create machinery survive that round trip every day. Moving a ship is the same round
 * trip with a different level on the far side: save the ship and everything attached to it (its
 * loading dependency chain, as Sable's {@code SubLevelHoldingChunkMap} does), take it out of the
 * source level, rewrite each saved pose to the destination, refile its blocks for the destination's
 * height range ({@link PlotSections#restacked}), and load it there. Players aboard move by the same
 * offset, so they stand where they stood.
 *
 * <p><strong>Its entities go with it.</strong> Sable's save holds blocks only. The entities living
 * inside a ship (Aeronautics' spinning propellers, Create contraptions and glue, seats, item frames)
 * are kept by the level, and taking a ship out of a level kills or drops them. So they are saved
 * and taken out first, and added back on the far side at the same place inside the ship: a ship's
 * storage slot has the same coordinates in every level.
 *
 * <p><strong>Internal Sable code, so version-pinned.</strong> The serializer is not in Sable's
 * {@code api} package. {@code sable_version_range} admits only the versions this was read against.
 *
 * <p><strong>Checked before anything moves.</strong> A saved ship remembers its storage slot (its
 * plot), and loading into a level where that slot is taken would fail halfway, with the ship
 * already gone from the source. So every part's slot is checked free in the destination first,
 * and the transfer is refused whole if any is not. New ships are given slots free in every level
 * ({@code SubLevelContainerMixin}, ADR-0016), so only ships built before this jar was installed
 * can still meet a taken slot. Should Sable still fail to load a part, the ship is put back where
 * it was.
 */
public final class ShipTransfer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipTransfer.class);

    /** How far outside a ship's bounds a player still counts as aboard (standing on the deck). */
    private static final double ABOARD_MARGIN = 1.5;

    private ShipTransfer() {
    }

    /**
     * A ship about to cross: the ship, everything that loads with it (the ship first), and the
     * players aboard. Gathered once, for one crossing, and never kept.
     */
    record Boarding(ServerSubLevel ship, List<ServerSubLevel> parts, List<ServerPlayer> crew) {
    }

    /** Gathers {@code ship}, its attached parts and its crew. */
    static Boarding board(ServerSubLevel ship) {
        List<ServerSubLevel> parts = new ArrayList<>(SubLevelHelper.getLoadingDependencyChain(ship));
        parts.remove(ship);
        parts.add(0, ship);
        List<ServerPlayer> crew = new ArrayList<>();
        for (ServerPlayer player : ship.getLevel().players()) {
            if (parts.stream().anyMatch(part -> aboard(part.boundingBox(), player.position()))) {
                crew.add(player);
            }
        }
        return new Boarding(ship, List.copyOf(parts), List.copyOf(crew));
    }

    /** The ship whose bounds contain {@code position}, with a little margin for standing on top. */
    @Nullable
    static ServerSubLevel shipAround(ServerLevel level, Vec3 position) {
        ServerSubLevelContainer ships = SubLevelContainer.getContainer(level);
        if (ships == null) {
            return null;
        }
        for (ServerSubLevel ship : ships.getAllSubLevels()) {
            if (aboard(ship.boundingBox(), position)) {
                return ship;
            }
        }
        return null;
    }

    /**
     * Carries the boarded ship, everything attached to it and everyone aboard, to {@code target}
     * with the ship's origin at {@code destination}. Returns null on success, or why not.
     */
    @Nullable
    static Component carry(Boarding boarding, ServerLevel target, Vec3 destination) {
        ServerLevel source = boarding.ship().getLevel();
        ServerSubLevelContainer sourceShips = SubLevelContainer.getContainer(source);
        ServerSubLevelContainer targetShips = SubLevelContainer.getContainer(target);
        if (sourceShips == null || targetShips == null) {
            return Component.literal("Ships cannot exist in " + target.dimension().location() + ".");
        }
        Vec3 offset = destination.subtract(toVec3(boarding.ship().logicalPose().position()));

        List<UUID> chain = boarding.parts().stream().map(ServerSubLevel::getUniqueId).toList();
        List<SubLevelData> saved = new ArrayList<>();
        List<SubLevelData> moved = new ArrayList<>();
        for (ServerSubLevel part : boarding.parts()) {
            SubLevelData data = SubLevelSerializer.toData(part, chain);
            CompoundTag plot = data.fullTag().getCompound("plot");
            // Sable's occupancy record, not just the loaded ships: a ship parked out of range is
            // unloaded but still owns its slot, and loading over it would cost that ship its place.
            if (targetShips.getOccupancy().get(targetShips.getIndex(plot.getInt("plot_x"), plot.getInt("plot_z")))) {
                return Component.literal("Another ship holds this ship's storage slot in "
                        + target.dimension().location() + "; not moved.");
            }
            SubLevelData there = relocated(data, offset, source, target);
            if (there == null) {
                return Component.literal("This ship does not fit within the height of "
                        + target.dimension().location() + "; not moved.");
            }
            saved.add(data);
            moved.add(there);
        }

        boarding.crew().forEach(Entity::stopRiding);
        List<CompoundTag> cargo = takeEntities(source, boarding.parts());
        target.getChunkAt(BlockPos.containing(destination));
        boarding.parts().forEach(part -> sourceShips.removeSubLevel(part, SubLevelRemovalReason.REMOVED));

        List<ServerSubLevel> loaded = new ArrayList<>();
        for (SubLevelData data : moved) {
            ServerSubLevel part = SubLevelSerializer.fullyLoad(target, data);
            if (part == null) {
                putBack(source, saved, cargo, targetShips, loaded);
                return Component.literal("Sable could not load this ship in " + target.dimension().location()
                        + ", so it stayed where it was; see the server log.");
            }
            loaded.add(part);
        }
        addEntities(target, cargo);
        for (ServerPlayer member : boarding.crew()) {
            Vec3 there = member.position().add(offset);
            member.teleportTo(target, there.x, there.y, there.z, member.getYRot(), member.getXRot());
        }
        return null;
    }

    /**
     * Saves and removes every entity living inside the ship's parts, players aside (they are crew).
     * Removed as having changed dimension, so nothing treats them as destroyed. Saved whole, riders
     * included, by their root.
     */
    private static List<CompoundTag> takeEntities(ServerLevel level, List<ServerSubLevel> parts) {
        List<CompoundTag> cargo = new ArrayList<>();
        for (ServerSubLevel part : parts) {
            ChunkPos min = part.getPlot().getChunkMin();
            ChunkPos max = part.getPlot().getChunkMax();
            AABB plot = new AABB(min.getMinBlockX(), level.getMinBuildHeight(), min.getMinBlockZ(),
                    max.getMaxBlockX() + 1, level.getMaxBuildHeight(), max.getMaxBlockZ() + 1);
            for (Entity entity : level.getEntities((Entity) null, plot, e -> !(e instanceof Player) && !e.isPassenger())) {
                CompoundTag tag = new CompoundTag();
                if (entity.save(tag)) {
                    cargo.add(tag);
                }
                entity.getPassengersAndSelf().forEach(each -> each.remove(Entity.RemovalReason.CHANGED_DIMENSION));
            }
        }
        return cargo;
    }

    /** Adds saved entities back at the place inside the ship they were saved from. */
    private static void addEntities(ServerLevel level, List<CompoundTag> cargo) {
        for (CompoundTag tag : cargo) {
            Entity entity = EntityType.loadEntityRecursive(tag, level, loaded -> loaded);
            if (entity == null || !level.tryAddFreshEntityWithPassengers(entity)) {
                LOGGER.warn("Could not bring entity {} along with its ship into {}",
                        tag.getString("id"), level.dimension().location());
            }
        }
    }

    /**
     * Undoes a crossing that failed halfway: parts already loaded on the far side are taken out
     * again, and the ship and its entities go back where they were saved from.
     */
    private static void putBack(ServerLevel source, List<SubLevelData> saved, List<CompoundTag> cargo,
            ServerSubLevelContainer targetShips, List<ServerSubLevel> loaded) {
        loaded.forEach(part -> targetShips.removeSubLevel(part, SubLevelRemovalReason.REMOVED));
        for (SubLevelData data : saved) {
            if (SubLevelSerializer.fullyLoad(source, data) == null) {
                LOGGER.error("Ship part {} could not be put back into {} after a failed crossing; it is lost",
                        data.uuid(), source.dimension().location());
            }
        }
        addEntities(source, cargo);
    }

    private static boolean aboard(BoundingBox3dc bounds, Vec3 position) {
        return position.x >= bounds.minX() - ABOARD_MARGIN && position.x <= bounds.maxX() + ABOARD_MARGIN
                && position.y >= bounds.minY() - ABOARD_MARGIN && position.y <= bounds.maxY() + ABOARD_MARGIN
                && position.z >= bounds.minZ() - ABOARD_MARGIN && position.z <= bounds.maxZ() + ABOARD_MARGIN;
    }

    /**
     * The same saved ship with its pose moved by {@code offset}, orientation unchanged, and its
     * blocks filed the way {@code target} reads them. Null if they would not fit there.
     */
    @Nullable
    private static SubLevelData relocated(SubLevelData data, Vec3 offset, ServerLevel source, ServerLevel target) {
        CompoundTag tag = data.fullTag().copy();
        CompoundTag plot = PlotSections.restacked(tag.getCompound("plot"), source, target);
        if (plot == null) {
            return null;
        }
        tag.put("plot", plot);
        Pose3d pose = SableNBTUtils.readPose3d(tag.getCompound("pose"));
        pose.position().add(offset.x, offset.y, offset.z);
        tag.put("pose", SableNBTUtils.writePose3d(pose));
        return new SubLevelData(data.uuid(), data.bounds(), pose, data.dependencies(), tag);
    }

    private static Vec3 toVec3(org.joml.Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
