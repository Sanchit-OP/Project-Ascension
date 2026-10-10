package com.ascension.worlds.internal.net;

import com.ascension.worlds.internal.GatewayDestinations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
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

    /**
     * Bump when any payload's shape changes. 2: the gateway destination screen. 3: archive reading.
     * 4: gate progress.
     */
    private static final String VERSION = "4";

    private WorldsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        toClient(registrar, PlanetWeatherSyncPayload.TYPE, PlanetWeatherSyncPayload.STREAM_CODEC,
                () -> com.ascension.worlds.client.ClientNetworkHandlers::planetWeatherSync);
        toClient(registrar, GatewayDestinationsPayload.TYPE, GatewayDestinationsPayload.STREAM_CODEC,
                () -> com.ascension.worlds.client.ClientNetworkHandlers::gatewayDestinations);
        toClient(registrar, OpenArchiveEntryPayload.TYPE, OpenArchiveEntryPayload.STREAM_CODEC,
                () -> com.ascension.worlds.client.ClientNetworkHandlers::openArchiveEntry);
        toClient(registrar, GateProgressPayload.TYPE, GateProgressPayload.STREAM_CODEC,
                () -> com.ascension.worlds.client.ClientNetworkHandlers::gateProgress);

        registrar.playToServer(
                GatewayTravelPayload.TYPE,
                GatewayTravelPayload.STREAM_CODEC,
                GatewayDestinations::onTravelRequest);
    }

    /**
     * A server &rarr; client payload: its client handler on a client, and on a dedicated server a
     * no-op (registered so the server can encode and send it; it is never received there).
     *
     * <p>The handler comes wrapped in a {@link Supplier} so that only a client ever evaluates the
     * method reference inside it. A dedicated server builds the supplier but never calls it, so it
     * never links anything in the client package: the same guarantee the old explicit
     * {@code if (dist == CLIENT)} branch gave, in one call per payload.
     */
    private static <T extends CustomPacketPayload> void toClient(
            PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Supplier<IPayloadHandler<T>> clientHandler) {
        IPayloadHandler<T> handler = FMLEnvironment.dist == Dist.CLIENT
                ? clientHandler.get()
                : (payload, context) -> {
                };
        registrar.playToClient(type, codec, handler);
    }
}
