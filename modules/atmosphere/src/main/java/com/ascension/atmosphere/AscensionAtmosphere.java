package com.ascension.atmosphere;

import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereCommands;
import com.ascension.atmosphere.internal.DebugAtmosphere;
import com.ascension.atmosphere.internal.OxygenTracker;
import com.ascension.atmosphere.internal.ProviderRegistry;
import com.ascension.atmosphere.internal.net.AtmosphereNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
        AtmosphereAttachments.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(AtmosphereNetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        LOGGER.info("Ascension Atmosphere loaded ({})", container.getModInfo().getVersion());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AtmosphereRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "debug_vacuum"),
                    new DebugAtmosphere());

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
}
