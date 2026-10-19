package com.ascension.combat.client;

import com.ascension.combat.internal.net.AbilityCooldownSyncPayload;
import com.ascension.combat.internal.net.RunePageResultPayload;
import com.ascension.combat.internal.net.RunePageSyncPayload;
import com.ascension.combat.internal.net.StasisSyncPayload;
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
        context.enqueueWork(() -> ClientRunePageCache.update(payload));
    }

    public static void onRunePageResult(RunePageResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RuneScreen screen) {
                screen.onResult(payload.refusal());
            }
        });
    }

    public static void onRuneHudSync(com.ascension.combat.internal.net.RuneHudSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            long now = mc.level == null ? 0L : mc.level.getGameTime();
            com.ascension.combat.client.hud.ClientRuneHud.update(payload.entries(), now);
        });
    }

    public static void onMagicHit(com.ascension.combat.internal.net.MagicHitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.ascension.combat.client.hud.ClientMagicHit.pulse(payload.strength()));
    }

    public static void onAbilityCooldownSync(AbilityCooldownSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAbilityCooldowns.update(
                payload.key(), payload.endsAtGameTime(), payload.durationTicks()));
    }
}
