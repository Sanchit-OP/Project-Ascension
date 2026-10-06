package com.ascension.worlds.internal.net;

import com.ascension.worlds.internal.GatewayDestinations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for this module. Mirrors {@code ascension-atmosphere}'s
 * {@code AtmosphereNetwork} &mdash; both sides register, only the client handles the client-bound
 * payloads, and the dist branch keeps the server from ever linking a reference to client-only
 * code. See that class's javadoc for why the branch matters structurally and not just "in
 * practice". The one server-bound payload ({@link GatewayTravelPayload}) is handled by common code
 * on both dists, since an integrated server runs on the client dist too.
 */
public final class WorldsNetwork {

    /** Bump when any payload's shape changes. 2: the gateway destination screen. 3: archive reading. */
    private static final String VERSION = "3";

    private WorldsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    PlanetWeatherSyncPayload.TYPE,
                    PlanetWeatherSyncPayload.STREAM_CODEC,
                    com.ascension.worlds.client.ClientNetworkHandlers::planetWeatherSync);
            registrar.playToClient(
                    GatewayDestinationsPayload.TYPE,
                    GatewayDestinationsPayload.STREAM_CODEC,
                    com.ascension.worlds.client.ClientNetworkHandlers::gatewayDestinations);
            registrar.playToClient(
                    OpenArchiveEntryPayload.TYPE,
                    OpenArchiveEntryPayload.STREAM_CODEC,
                    com.ascension.worlds.client.ClientNetworkHandlers::openArchiveEntry);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(
                    PlanetWeatherSyncPayload.TYPE,
                    PlanetWeatherSyncPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
            registrar.playToClient(
                    GatewayDestinationsPayload.TYPE,
                    GatewayDestinationsPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
            registrar.playToClient(
                    OpenArchiveEntryPayload.TYPE,
                    OpenArchiveEntryPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
        }

        registrar.playToServer(
                GatewayTravelPayload.TYPE,
                GatewayTravelPayload.STREAM_CODEC,
                GatewayDestinations::onTravelRequest);
    }
}
