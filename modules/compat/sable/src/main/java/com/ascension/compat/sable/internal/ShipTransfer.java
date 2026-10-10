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
 * source level, rewrite each saved pose to the destination, refile its blocks for the destination's
 * height range ({@link #restacked}), and load it there. Players aboard
 * move by the same offset, so they stand where they stood.
 *
 * <p><strong>Internal Sable code, so version-pinned.</strong> The serializer is not in Sable's
 * {@code api} package. {@code sable_version_range} admits only the versions this was read against.
 *
 * <p><strong>Checked before anything moves.</strong> A saved ship remembers its storage slot (its
 * plot), and loading into a level where that slot is taken would fail halfway, with the ship
 * already gone from the source. So every part's slot is checked free in the destination first,
 * and the transfer is refused whole if any is not. New ships are given slots free in every level
 * ({@code SubLevelContainerMixin}, ADR-0016), so only ships built before this jar was installed
 * can still meet a taken slot.
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
        ServerSubLevel ship = shipAround(player.serverLevel(), player.position());
        if (ship == null) {
            return Component.literal("You are not aboard a ship.");
        }
        return carry(ship, target, destination);
    }

    /**
     * Carries {@code ship}, everything attached to it and everyone aboard, to {@code target} with
     * the ship's origin at {@code destination}. Returns null on success, or why not.
     */
    @Nullable
    public static Component carry(ServerSubLevel ship, ServerLevel target, Vec3 destination) {
        ServerLevel source = ship.getLevel();
        List<ServerSubLevel> parts = partsOf(ship);
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
            moved.add(there);
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

    /** Everyone aboard {@code ship} or anything attached to it. */
    static List<ServerPlayer> crewOf(ServerSubLevel ship) {
        return crewOf(ship.getLevel(), partsOf(ship));
    }

    /** The ship and everything that loads with it, the ship first. */
    private static List<ServerSubLevel> partsOf(ServerSubLevel ship) {
        List<ServerSubLevel> parts = new ArrayList<>(SubLevelHelper.getLoadingDependencyChain(ship));
        parts.remove(ship);
        parts.add(0, ship);
        return parts;
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

    /**
     * The same saved ship with its pose moved by {@code offset}, orientation unchanged, and its
     * blocks filed the way {@code target} reads them. Null if they would not fit there.
     */
    @Nullable
    private static SubLevelData relocated(SubLevelData data, Vec3 offset, ServerLevel source, ServerLevel target) {
        CompoundTag tag = data.fullTag().copy();
        CompoundTag plot = restacked(tag.getCompound("plot"), source, target);
        if (plot == null) {
            return null;
        }
        tag.put("plot", plot);
        Pose3d pose = SableNBTUtils.readPose3d(tag.getCompound("pose"));
        pose.position().add(offset.x, offset.y, offset.z);
        tag.put("pose", SableNBTUtils.writePose3d(pose));
        return new SubLevelData(data.uuid(), data.bounds(), pose, data.dependencies(), tag);
    }

    /**
     * Sable files a ship's blocks by section number, counted up from the bottom of the level they
     * were saved in. Earth and the Moon start at y -64, space at y -512, so the same number is a
     * different height on each side: read unchanged, a ship's blocks sank 448 blocks inside its own
     * storage, away from its chests' contents, which are saved by height. So every section is
     * renumbered to keep its height, and the saved heightmaps, sized for the old level's height,
     * are dropped for Sable to rebuild. Null if a section would land outside {@code target}.
     */
    @Nullable
    private static CompoundTag restacked(CompoundTag plot, ServerLevel source, ServerLevel target) {
        int shift = source.getMinSection() - target.getMinSection();
        if (shift == 0 && source.getSectionsCount() == target.getSectionsCount()) {
            return plot;
        }
        CompoundTag chunks = plot.getCompound("chunks");
        CompoundTag restackedChunks = new CompoundTag();
        for (String key : chunks.getAllKeys()) {
            CompoundTag chunk = chunks.getCompound(key);
            CompoundTag sections = chunk.getCompound("sections");
            CompoundTag renumbered = new CompoundTag();
            for (String index : sections.getAllKeys()) {
                int to = Integer.parseInt(index) + shift;
                if (to < 0 || to >= target.getSectionsCount()) {
                    return null;
                }
                renumbered.put(String.valueOf(to), sections.get(index));
            }
            chunk.put("sections", renumbered);
            chunk.remove("heightmaps");
            restackedChunks.put(key, chunk);
        }
        plot.put("chunks", restackedChunks);
        return plot;
    }

    private static Vec3 toVec3(org.joml.Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
