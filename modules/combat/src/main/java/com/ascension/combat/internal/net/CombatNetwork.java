package com.ascension.combat.internal.net;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.armor.GargoyleStoneplateEffects;
import com.ascension.combat.internal.armor.RocketbeltEffects;
import com.ascension.combat.internal.armor.ZoniasHourglassEffects;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.RunePageState;
import com.ascension.combat.internal.rune.Runes;
import com.ascension.combat.internal.weapons.GaleforceEffects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for this module's network traffic: client-to-server packets ({@link
 * ActivateArmorAbilityPayload}, {@link SetRunePagePayload}) and server-to-client packets ({@link
 * StasisSyncPayload}, {@link RunePageSyncPayload}, {@link RunePageResultPayload}).
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
    private static final String VERSION = "4";

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
            registrar.playToClient(
                    RunePageResultPayload.TYPE,
                    RunePageResultPayload.STREAM_CODEC,
                    com.ascension.combat.client.ClientNetworkHandlers::onRunePageResult);
            registrar.playToClient(
                    AbilityCooldownSyncPayload.TYPE,
                    AbilityCooldownSyncPayload.STREAM_CODEC,
                    com.ascension.combat.client.ClientNetworkHandlers::onAbilityCooldownSync);
        } else {
            // Registered so the server can encode and send; never received here.
            registrar.playToClient(StasisSyncPayload.TYPE, StasisSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(RunePageSyncPayload.TYPE, RunePageSyncPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(RunePageResultPayload.TYPE, RunePageResultPayload.STREAM_CODEC, (payload, context) -> {
            });
            registrar.playToClient(
                    AbilityCooldownSyncPayload.TYPE, AbilityCooldownSyncPayload.STREAM_CODEC, (payload, context) -> {
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
            if (context.player() instanceof ServerPlayer player) {
                Runes.save(player, payload.page());
            }
        });
    }

    /**
     * The one moment a client's rune-page cache would otherwise go stale: connecting in the first
     * place. A rune page never changes on its own from a dimension change, so login is the only
     * trigger besides a save. It is also where a page saved before keystones were a choice gets the
     * keystone it had then: only here is the rune registry at hand to say which that was.
     */
    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            state.fillMissingKeystone(Runes.registry(player));
            RuneModifiers.refresh(player);
            Runes.sync(player);
        }
    }
}
