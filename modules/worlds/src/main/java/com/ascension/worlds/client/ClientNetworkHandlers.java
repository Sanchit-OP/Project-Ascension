package com.ascension.worlds.client;

import com.ascension.worlds.internal.net.GatewayDestinationsPayload;
import com.ascension.worlds.internal.net.OpenArchiveEntryPayload;
import com.ascension.worlds.internal.net.PlanetWeatherSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side payload handling. Separated from {@code WorldsNetwork} so that module's server
 * branch of registration never names a client class it might have to load, matching
 * {@code ascension-atmosphere}'s own split.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandlers {

    private ClientNetworkHandlers() {
    }

    public static void planetWeatherSync(PlanetWeatherSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlanetWeather.accept(payload));
    }

    public static void gatewayDestinations(GatewayDestinationsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new GatewayDestinationScreen(payload.destinations())));
    }

    public static void openArchiveEntry(OpenArchiveEntryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ArchiveReader.open(payload.entry().orElse(null)));
    }
}
