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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Moves a ship from one level to another, crew and all, keeping its orientation: the one thing
 * Sable has no call for, and the thing a ship flying from a planet into space needs.
 *
 * <p><strong>Through Sable's own save and load.</strong> A ship whose chunks unload is written out
 * by {@code SubLevelSerializer.toData} and read back by {@code fullyLoad}, so its blocks, block
 * entities and Create machinery survive that round trip every day. Moving a ship is the same round
 * trip with a different level on the far side: save the ship and everything attached to it (its
 * loading dependency chain, as Sable's {@code SubLevelHoldingChunkMap} does), take it out of the
 * source level, rewrite each saved pose to the destination, and load it there. Players aboard
 * move by the same offset, so they stand where they stood.
 *
 * <p><strong>Internal Sable code, so version-pinned.</strong> The serializer is not in Sable's
 * {@code api} package. {@code sable_version_range} admits only the versions this was read against.
 *
 * <p><strong>Checked before anything moves.</strong> A saved ship remembers its storage slot (its
 * plot), and loading into a level where that slot is taken would fail halfway, with the ship
 * already gone from the source. So every part's slot is checked free in the destination first,
 * and the transfer is refused whole if any is not.
 */
public final class ShipTransfer {

    /** How far outside a ship's bounds a player still counts as aboard (standing on the deck). */
    private static final double ABOARD_MARGIN = 1.5;

    private ShipTransfer() {
    }

    /**
     * Carries the ship {@code player} stands on, with everyone aboard, to {@code target} so the
     * ship's origin lands at {@code destination}. Returns null on success, or why not.
     */
    @Nullable
    public static Component carry(ServerPlayer player, ServerLevel target, Vec3 destination) {
        ServerLevel source = player.serverLevel();
        ServerSubLevel ship = shipAround(source, player.position());
        if (ship == null) {
            return Component.literal("You are not aboard a ship.");
        }
        List<ServerSubLevel> parts = new ArrayList<>(SubLevelHelper.getLoadingDependencyChain(ship));
        if (!parts.contains(ship)) {
            parts.add(0, ship);
        }
        Vec3 from = toVec3(ship.logicalPose().position());
        Vec3 offset = destination.subtract(from);

        ServerSubLevelContainer targetShips = SubLevelContainer.getContainer(target);
        if (targetShips == null) {
            return Component.literal("Ships cannot exist in " + target.dimension().location() + ".");
        }
        List<UUID> chain = parts.stream().map(ServerSubLevel::getUniqueId).toList();
        List<SubLevelData> moved = new ArrayList<>();
        for (ServerSubLevel part : parts) {
            SubLevelData data = SubLevelSerializer.toData(part, chain);
            CompoundTag plot = data.fullTag().getCompound("plot");
            if (targetShips.getSubLevel(plot.getInt("plot_x"), plot.getInt("plot_z")) != null) {
                return Component.literal("A ship already holds this ship's storage slot in "
                        + target.dimension().location() + "; not moved.");
            }
            moved.add(relocated(data, offset));
        }
        List<ServerPlayer> crew = crewOf(source, parts);

        target.getChunkAt(BlockPos.containing(destination));
        ServerSubLevelContainer sourceShips = SubLevelContainer.getContainer(source);
        for (ServerSubLevel part : parts) {
            sourceShips.removeSubLevel(part, SubLevelRemovalReason.REMOVED);
        }
        for (SubLevelData data : moved) {
            if (SubLevelSerializer.fullyLoad(target, data) == null) {
                return Component.literal("Sable could not load part " + data.uuid() + " in "
                        + target.dimension().location() + "; see the server log.");
            }
        }
        for (ServerPlayer member : crew) {
            Vec3 there = member.position().add(offset);
            member.teleportTo(target, there.x, there.y, there.z, member.getYRot(), member.getXRot());
        }
        return null;
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

    private static List<ServerPlayer> crewOf(ServerLevel level, List<ServerSubLevel> parts) {
        List<ServerPlayer> crew = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            for (ServerSubLevel part : parts) {
                if (aboard(part.boundingBox(), player.position())) {
                    crew.add(player);
                    break;
                }
            }
        }
        return crew;
    }

    private static boolean aboard(BoundingBox3dc bounds, Vec3 position) {
        return position.x >= bounds.minX() - ABOARD_MARGIN && position.x <= bounds.maxX() + ABOARD_MARGIN
                && position.y >= bounds.minY() - ABOARD_MARGIN && position.y <= bounds.maxY() + ABOARD_MARGIN
                && position.z >= bounds.minZ() - ABOARD_MARGIN && position.z <= bounds.maxZ() + ABOARD_MARGIN;
    }

    /** The same saved ship with its pose moved by {@code offset}; orientation unchanged. */
    private static SubLevelData relocated(SubLevelData data, Vec3 offset) {
        CompoundTag tag = data.fullTag().copy();
        Pose3d pose = SableNBTUtils.readPose3d(tag.getCompound("pose"));
        pose.position().add(offset.x, offset.y, offset.z);
        tag.put("pose", SableNBTUtils.writePose3d(pose));
        return new SubLevelData(data.uuid(), data.bounds(), pose, data.dependencies(), tag);
    }

    private static Vec3 toVec3(org.joml.Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
