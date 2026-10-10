package com.ascension.compat.sable.internal;

import com.ascension.compat.sable.AscensionCompatSable;
import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.SpaceTravel;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The airship road to space (ADR-0015): a ship that climbs past a planet's build height is carried
 * out into space, crew and all, and a ship that reaches a planet's body in space is carried down to
 * that planet's sky.
 *
 * <p><strong>The same line players cross.</strong> Where a ship goes is asked of ascension-worlds
 * ({@link SpaceTravel}), so it comes out in space where a player rising on foot would, along the
 * line it was flying, and comes down over the spot its pilot last left from. Its riders stay
 * aboard: the carrier registered here tells worlds not to move them on its own.
 *
 * <p><strong>Once a second, ships only.</strong> The ships loaded in each planet's sky and in space
 * are checked against their line once a second: a short list of ships, never a search of the world
 * (ADR-0007). Climbing past the line takes a ship several seconds anyway.
 *
 * <p>A refused crossing (Sable cannot load the ship on the far side) is told to the crew at every
 * check while the ship stays over the line, and logged once per ship. Those ship ids are the only
 * state kept, emptied when the server stops.
 */
public final class ShipVoyages {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipVoyages.class);

    /** Ships whose refused crossing is already in the log, so one stuck ship logs once. */
    private static final Set<UUID> LOGGED_REFUSALS = new HashSet<>();

    private ShipVoyages() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ShipVoyages::onServerTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> LOGGED_REFUSALS.clear());
        SpaceTravel.registerCarrier(
                ResourceLocation.fromNamespaceAndPath(AscensionCompatSable.MOD_ID, "ship"), ShipVoyages::aboard);
    }

    /** Anyone standing on a ship goes when the ship goes. */
    private static boolean aboard(ServerPlayer player) {
        return ShipTransfer.shipAround(player.serverLevel(), player.position()) != null;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel space = server.getLevel(SpaceTravel.SPACE);
        if (space == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level == space) {
                checkDescents(server, space);
            } else {
                SpaceTravel.planetWithSurface(server, level.dimension())
                        .ifPresent(planet -> checkAscents(level, planet, space));
            }
        }
    }

    private static void checkAscents(ServerLevel surface, Planet planet, ServerLevel space) {
        double ceiling = surface.getMaxBuildHeight();
        for (ServerSubLevel ship : shipsIn(surface)) {
            if (gone(ship) || ship.logicalPose().position().y() < ceiling) {
                continue;
            }
            List<ServerPlayer> crew = ShipTransfer.crewOf(ship);
            Vector3dc velocity = ship.latestLinearVelocity;
            float heading = ShipHeading.yaw(velocity.x(), velocity.z(), crew.isEmpty() ? 0.0F : crew.get(0).getYRot());
            // Remembered as they leave, so the ship that brings them back comes down over this spot.
            crew.forEach(SpaceTravel::noteDeparture);
            report(ship, crew, ShipTransfer.carry(ship, space, SpaceTravel.arrivalInSpace(planet, heading)));
        }
    }

    private static void checkDescents(MinecraftServer server, ServerLevel space) {
        for (ServerSubLevel ship : shipsIn(space)) {
            if (gone(ship)) {
                continue;
            }
            Vector3dc at = ship.logicalPose().position();
            Planet planet = SpaceTravel.planetUnder(server, at.x(), at.z()).orElse(null);
            ServerLevel surface = planet == null ? null : server.getLevel(planet.surface());
            if (surface == null) {
                continue;
            }
            List<ServerPlayer> crew = ShipTransfer.crewOf(ship);
            Vec3 destination = SpaceTravel.arrivalAbove(surface, crew.isEmpty() ? null : crew.get(0));
            report(ship, crew, ShipTransfer.carry(ship, surface, destination));
        }
    }

    /** A copy: carrying a ship takes it, and everything attached to it, out of the live list. */
    private static List<ServerSubLevel> shipsIn(ServerLevel level) {
        ServerSubLevelContainer ships = SubLevelContainer.getContainer(level);
        return ships == null ? List.of() : List.copyOf(ships.getAllSubLevels());
    }

    /** Already carried during this check, attached to a ship that went before it. */
    private static boolean gone(ServerSubLevel ship) {
        ServerSubLevelContainer ships = SubLevelContainer.getContainer(ship.getLevel());
        return ships == null || ships.getSubLevel(ship.getUniqueId()) != ship;
    }

    private static void report(ServerSubLevel ship, List<ServerPlayer> crew, @Nullable Component refusal) {
        if (refusal == null) {
            LOGGED_REFUSALS.remove(ship.getUniqueId());
            return;
        }
        crew.forEach(member -> member.displayClientMessage(refusal, true));
        if (LOGGED_REFUSALS.add(ship.getUniqueId())) {
            LOGGER.warn("Ship {} cannot cross out of {}: {}",
                    ship.getUniqueId(), ship.getLevel().dimension().location(), refusal.getString());
        }
    }
}
