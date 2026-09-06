package com.ascension.worlds.internal.net;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for this module. Mirrors {@code ascension-atmosphere}'s
 * {@code AtmosphereNetwork} &mdash; both sides register, only the client handles, and the dist
 * branch keeps the server from ever linking a reference to client-only code. See that class's
 * javadoc for why the branch matters structurally and not just "in practice".
 */
public final class WorldsNetwork {

    /** Bump when {@link PlanetWeatherSyncPayload}'s shape changes. */
    private static final String VERSION = "1";

    private WorldsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    PlanetWeatherSyncPayload.TYPE,
                    PlanetWeatherSyncPayload.STREAM_CODEC,
                    com.ascension.worlds.client.ClientNetworkHandlers::planetWeatherSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(
                    PlanetWeatherSyncPayload.TYPE,
                    PlanetWeatherSyncPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
        }
    }
}
