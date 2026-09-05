package com.ascension.atmosphere.internal.net;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration.
 *
 * <p>Both sides must register the payload: the client to decode it, the server to encode it
 * when sending. Only the client can meaningfully handle it.
 *
 * <p><strong>Why the dist branch.</strong> A handler lambda that calls client code compiles a
 * reference to that client class into this class. In practice it never loads on a dedicated
 * server, because a play-to-client handler never runs there and the JVM resolves lazily &mdash;
 * but "in practice, because of lazy resolution" is not a guarantee worth building a dedicated
 * server on. The branch below makes it structural: on a server, the client method reference is
 * never linked, so the class is never loaded, and that holds regardless of JVM resolution
 * behaviour.
 *
 * <p>This is the single place in the module where non-client code names a client class, and it
 * is deliberately isolated here so the rule stays easy to audit.
 */
public final class AtmosphereNetwork {

    /**
     * Protocol version. Bump when the payload shape changes, so mismatched clients are rejected
     * at handshake rather than silently misreading a packet.
     */
    private static final String VERSION = "1";

    private AtmosphereNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    OxygenSyncPayload.TYPE,
                    OxygenSyncPayload.STREAM_CODEC,
                    com.ascension.atmosphere.client.ClientNetworkHandlers::oxygenSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(
                    OxygenSyncPayload.TYPE,
                    OxygenSyncPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
        }
    }
}
