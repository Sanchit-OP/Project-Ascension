package com.ascension.combat.internal.net;

import com.ascension.combat.internal.armor.GargoyleStoneplateEffects;
import com.ascension.combat.internal.armor.RocketbeltEffects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for this module's network traffic: one client-to-server packet ({@link
 * ActivateArmorAbilityPayload}) and one server-to-client packet ({@link StasisSyncPayload}).
 *
 * <p>Only the S2C direction needs the {@code Dist} branch {@code AtmosphereNetwork} uses &mdash; a
 * {@code playToServer} handler only ever runs on the server and can reference server-side classes
 * directly, but a {@code playToClient} handler referencing a client class would otherwise compile
 * a reference to that class into code the server also loads. See {@code AtmosphereNetwork}'s own
 * javadoc for the full reasoning; this class is the second place in the project that needs it, not
 * a new pattern.
 */
public final class CombatNetwork {

    /**
     * Protocol version. Bump when the set of payloads or any payload's shape changes, so
     * mismatched clients are rejected at handshake rather than silently misreading a packet.
     */
    private static final String VERSION = "2";

    private CombatNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(
                ActivateArmorAbilityPayload.TYPE,
                ActivateArmorAbilityPayload.STREAM_CODEC,
                CombatNetwork::onActivateArmorAbility);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    StasisSyncPayload.TYPE,
                    StasisSyncPayload.STREAM_CODEC,
                    com.ascension.combat.client.ClientNetworkHandlers::onStasisSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(StasisSyncPayload.TYPE, StasisSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
        }
    }

    private static void onActivateArmorAbility(ActivateArmorAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GargoyleStoneplateEffects.tryActivate(player);
                RocketbeltEffects.tryActivate(player);
            }
        });
    }
}
