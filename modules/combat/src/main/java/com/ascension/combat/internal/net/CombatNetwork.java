package com.ascension.combat.internal.net;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.armor.GargoyleStoneplateEffects;
import com.ascension.combat.internal.armor.RocketbeltEffects;
import com.ascension.combat.internal.armor.ZoniasHourglassEffects;
import com.ascension.combat.internal.rune.RunePageState;
import com.ascension.combat.internal.weapons.GaleforceEffects;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for this module's network traffic: client-to-server packets ({@link
 * ActivateArmorAbilityPayload}, {@link SetRunePagePayload}) and server-to-client packets ({@link
 * StasisSyncPayload}, {@link RunePageSyncPayload}).
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
    private static final String VERSION = "3";

    private CombatNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(
                ActivateArmorAbilityPayload.TYPE,
                ActivateArmorAbilityPayload.STREAM_CODEC,
                CombatNetwork::onActivateArmorAbility);
        registrar.playToServer(SetRunePagePayload.TYPE, SetRunePagePayload.STREAM_CODEC, CombatNetwork::onSetRunePage);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    StasisSyncPayload.TYPE,
                    StasisSyncPayload.STREAM_CODEC,
                    com.ascension.combat.client.ClientNetworkHandlers::onStasisSync);
            registrar.playToClient(
                    RunePageSyncPayload.TYPE,
                    RunePageSyncPayload.STREAM_CODEC,
                    com.ascension.combat.client.ClientNetworkHandlers::onRunePageSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(StasisSyncPayload.TYPE, StasisSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(RunePageSyncPayload.TYPE, RunePageSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
        }
    }

    /** Attached to the game bus separately from {@link #register}, which only handles the mod bus. */
    public static void registerLoginSync(IEventBus gameBus) {
        gameBus.addListener(CombatNetwork::onPlayerLoggedIn);
    }

    private static void onActivateArmorAbility(ActivateArmorAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GargoyleStoneplateEffects.tryActivate(player);
                RocketbeltEffects.tryActivate(player);
                ZoniasHourglassEffects.tryActivate(player);
                GaleforceEffects.tryActivate(player);
            }
        });
    }

    private static void onSetRunePage(SetRunePagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            boolean applied = state.trySetPage(
                    payload.primaryPath(),
                    Set.copyOf(payload.primaryMinors()),
                    payload.secondaryPath(),
                    Set.copyOf(payload.secondaryMinors()));
            if (applied) {
                PacketDistributor.sendToPlayer(player, RunePageSyncPayload.from(state));
            }
        });
    }

    /**
     * The one moment a client's rune-page cache would otherwise go stale: connecting in the first
     * place. Unlike {@code PlanetWeatherMechanics}, a rune page never changes on its own from a
     * dimension change, so login is the only trigger this needs &mdash; every other update already
     * echoes back through {@link #onSetRunePage} the instant the player themselves saves one.
     */
    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            PacketDistributor.sendToPlayer(player, RunePageSyncPayload.from(state));
        }
    }
}
