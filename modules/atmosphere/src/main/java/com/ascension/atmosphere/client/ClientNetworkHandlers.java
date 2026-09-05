package com.ascension.atmosphere.client;

import com.ascension.atmosphere.internal.net.OxygenSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side payload handling.
 *
 * <p>Separated from {@code AtmosphereNetwork} so that the server branch of registration never
 * names a client class it might have to load.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandlers {

    private ClientNetworkHandlers() {
    }

    public static void oxygenSync(OxygenSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientOxygenState.accept(payload));
    }
}
