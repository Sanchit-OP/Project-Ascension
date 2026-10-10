package com.ascension.worlds.api;

import com.ascension.worlds.internal.AscentCarriers;
import com.ascension.worlds.internal.SpaceDimension;
import com.ascension.worlds.internal.SpaceMechanics;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The line between a planet's surface and space, for a vehicle that crosses it with its crew.
 *
 * <p>This module moves a player who rises past a planet's build height into space, and one who
 * reaches a planet's body in space down to its surface. A vehicle mod moves its own vehicles across
 * the same line, and asks here where it leads, so a ship comes out exactly where a player on foot
 * would and lands where its pilot would. It also registers an {@link AscentCarrier}, so this module
 * leaves the crew aboard to travel with the ship.
 *
 * <p>Server thread only, like the levels it hands back.
 */
public final class SpaceTravel {

    /** The one space dimension every planet sits in (ADR-0010). */
    public static final ResourceKey<Level> SPACE = SpaceDimension.KEY;

    private SpaceTravel() {
    }

    /**
     * Registers a vehicle that carries its riders across planet boundaries itself. Call it during
     * mod construction.
     *
     * @param id stable identifier, conventionally {@code yourmod:vehicle}
     * @throws IllegalArgumentException if {@code id} is already registered
     */
    public static void registerCarrier(ResourceLocation id, AscentCarrier carrier) {
        AscentCarriers.register(id, carrier);
    }

    /** The planet whose surface is {@code dimension}, if it is one. */
    public static Optional<Planet> planetWithSurface(MinecraftServer server, ResourceKey<Level> dimension) {
        return SpaceMechanics.planetWithSurface(server, dimension);
    }

    /**
     * The planet whose body (its {@code body_radius} in space) holds this point. Reaching a planet's
     * body is arriving there: a vehicle at this point goes down to that planet's surface.
     */
    public static Optional<Planet> planetUnder(MinecraftServer server, double x, double z) {
        return SpaceMechanics.planetUnder(server, x, z);
    }

    /**
     * Where something rising out of {@code planet}'s sky comes out in space: past the planet's
     * approach shell, along {@code yaw} (Minecraft's degrees, 0 = south), at height 0. The same
     * place a player rising on foot comes out.
     */
    public static Vec3 arrivalInSpace(Planet planet, float yaw) {
        return SpaceMechanics.arrivalInSpace(planet, yaw);
    }

    /**
     * Where a vehicle coming down to {@code surface} arrives: well below the build height, over the
     * spot {@code pilot} last rose from that world, or over its centre if they never have (or there
     * is no pilot).
     */
    public static Vec3 arrivalAbove(ServerLevel surface, @Nullable ServerPlayer pilot) {
        return SpaceMechanics.vehicleArrival(surface, pilot);
    }

    /**
     * Remembers where {@code player} is leaving their current world from, as a player rising on foot
     * is remembered: their next arrival back there lands near it. Call it for each rider as their
     * vehicle leaves a planet.
     */
    public static void noteDeparture(ServerPlayer player) {
        SpaceMechanics.noteDeparture(player);
    }
}
