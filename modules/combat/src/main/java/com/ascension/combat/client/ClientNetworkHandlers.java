package com.ascension.combat.client;

import com.ascension.combat.internal.net.RunePageSyncPayload;
import com.ascension.combat.internal.net.StasisSyncPayload;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side handlers for payloads sent to this player. Mirrors {@code ClientNetworkHandlers} in
 * {@code ascension-atmosphere}: one static method per payload type, doing the minimum client-side
 * work and nothing else.
 */
public final class ClientNetworkHandlers {

    /** The camera mode to restore when stasis ends -- {@code null} means nothing is saved. */
    private static CameraType cameraTypeBeforeStasis;

    private ClientNetworkHandlers() {
    }

    public static void onStasisSync(StasisSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            if (payload.active()) {
                cameraTypeBeforeStasis = client.options.getCameraType();
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else if (cameraTypeBeforeStasis != null) {
                client.options.setCameraType(cameraTypeBeforeStasis);
                cameraTypeBeforeStasis = null;
            }
        });
    }

    public static void onRunePageSync(RunePageSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRunePageCache.update(
                payload.primaryPath(),
                Set.copyOf(payload.primaryMinors()),
                payload.secondaryPath(),
                Set.copyOf(payload.secondaryMinors())));
    }
}
