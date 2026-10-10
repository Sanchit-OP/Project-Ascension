package com.ascension.worlds.internal;

import com.ascension.worlds.api.AscentCarrier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The registered {@link AscentCarrier}s, and the rule about rising on foot they make possible.
 *
 * <p>A concurrent map because mods are constructed in parallel and a carrier registers from its
 * constructor. It holds the carriers themselves, never a level or a player (ADR-0007), and lives as
 * long as the mods that registered them, like any registry.
 */
public final class AscentCarriers {

    private static final Map<ResourceLocation, AscentCarrier> CARRIERS = new ConcurrentHashMap<>();

    private AscentCarriers() {
    }

    /** See {@link com.ascension.worlds.api.SpaceTravel#registerCarrier}. */
    public static void register(ResourceLocation id, AscentCarrier carrier) {
        if (CARRIERS.putIfAbsent(id, carrier) != null) {
            throw new IllegalArgumentException("Ascent carrier " + id + " is already registered");
        }
    }

    /** True when some vehicle will carry {@code player} across this boundary itself. */
    static boolean carries(ServerPlayer player) {
        for (AscentCarrier carrier : CARRIERS.values()) {
            if (carrier.carries(player)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code player} may rise into space with no vehicle under them. */
    static boolean onFootAllowed(ServerPlayer player) {
        boolean vehicleOnly = WorldsConfig.SPEC.isLoaded() && WorldsConfig.INSTANCE.vehicleOnly.getAsBoolean();
        return onFootAllowed(vehicleOnly, !CARRIERS.isEmpty(), player.isCreative() || player.isSpectator());
    }

    /**
     * The rule itself. {@code vehicleOnly} closes the on-foot route, but only once a vehicle exists
     * to take its place: with no carrier registered, closing it would leave space unreachable.
     * Creative and spectator players are never stopped.
     */
    static boolean onFootAllowed(boolean vehicleOnly, boolean vehicleExists, boolean creative) {
        return !vehicleOnly || !vehicleExists || creative;
    }
}
