package com.ascension.atmosphere;

import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.AtmosphereCommands;
import com.ascension.atmosphere.internal.AtmosphereConfig;
import com.ascension.atmosphere.internal.DebugAtmosphere;
import com.ascension.atmosphere.internal.OxygenTracker;
import com.ascension.atmosphere.internal.ProviderRegistry;
import com.ascension.atmosphere.internal.VanillaIntegration;
import com.ascension.atmosphere.internal.net.AtmosphereNetwork;
import com.ascension.atmosphere.internal.supply.PlayerTankCollector;
import com.ascension.atmosphere.internal.sealed.SealedVolumeEvents;
import com.ascension.atmosphere.internal.sealed.SealedVolumeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_atmosphere}.
 *
 * <p>Tier 1 in ADR-0003: it depends on nothing but NeoForge and must load and work with no
 * other Ascension module present. Anything that needs a third-party mod belongs in a Tier 2
 * {@code ascension-compat-*} jar, never here.
 *
 * <p>Design record: {@code docs/technical/atmosphere-api.md}.
 */
@Mod(AscensionAtmosphere.MOD_ID)
public final class AscensionAtmosphere {

    public static final String MOD_ID = "ascension_atmosphere";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionAtmosphere.class);

    public AscensionAtmosphere(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AtmosphereConfig.SPEC);

        AtmosphereAttachments.register(modBus);
        AtmosphereContent.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(AtmosphereNetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        SealedVolumeEvents.register(NeoForge.EVENT_BUS);

        LOGGER.info("Ascension Atmosphere loaded ({})", container.getModInfo().getVersion());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AtmosphereRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "debug_vacuum"),
                    new DebugAtmosphere());

            // Water is just another unbreathable atmosphere. Registered unconditionally; the
            // provider itself honours the config, so toggling it needs no restart.
            AtmosphereRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "water"),
                    new VanillaIntegration.WaterAtmosphere());
            // Sealed rooms beat the vacuum around them, and would in turn lose to a vehicle.
            AtmosphereRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "sealed_volume"),
                    new SealedVolumeProvider());

            // Tanks carried in the inventory. Registered exactly the way a third party would
            // register a curio slot or a suit module — this module has no privileged path.
            AtmosphereRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "inventory_tanks"),
                    new PlayerTankCollector());

            AtmosphereRegistry.register(new VanillaIntegration.ConduitPower());
            AtmosphereRegistry.registerLungCapacity(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "vanilla_gear"),
                    new VanillaIntegration.VanillaBreathingGear());

            // Sort once, then never again. Queries after this point allocate nothing.
            ProviderRegistry.get().freeze();
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AtmosphereCommands.register(event.getDispatcher());
    }

    /**
     * Oxygen accounting. Rate-limited inside the tracker rather than here, so the interval is
     * defined next to the rest of the tuning.
     */
    private void onServerTick(ServerTickEvent.Post event) {
        OxygenTracker.tick(event.getServer());
    }

    /**
     * Hold vanilla's air supply full, every tick.
     *
     * <p>This has to run per tick, not per accounting pass. Vanilla decrements air every single
     * tick underwater, so pinning it only twice a second let it drain and redraw bubbles in
     * between, which is what made the vanilla meter reappear and flicker against ours.
     *
     * <p>Cheap by construction: one comparison and at most one setter per player, with no
     * allocation and no world access.
     */
    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }

    /**
     * Respawn with a full set of lungs.
     *
     * <p>Lungs refill on their own, so this is only about the first few seconds: without it a
     * player who suffocated respawns mid-refill and, if they respawn anywhere near water, starts
     * drowning again before they have caught their breath. Vanilla hands back full air on
     * respawn and players expect the same.
     *
     * <p>Carried tanks are deliberately untouched — they persist or drop with the inventory
     * like any other item.
     */
    private void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = player.getData(AtmosphereAttachments.OXYGEN);
            state.setLungUnits(OxygenTracker.lungCapacity(player));
            state.setSuffocationTicks(0);
            OxygenTracker.invalidate(player);
        }
    }
}
