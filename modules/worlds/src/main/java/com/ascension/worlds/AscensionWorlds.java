package com.ascension.worlds;

import com.ascension.core.api.WorldEnvironmentRegistry;
import com.ascension.worlds.api.Planet;
import com.ascension.worlds.api.WorldsRegistries;
import com.ascension.worlds.internal.PlanetEnvironments;
import com.ascension.worlds.internal.WorldsContent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_worlds}.
 *
 * <p>Tier 1 in ADR-0003: it must load and work with only {@code core} present. Its one Ascension
 * dependency is {@code ascension_core}, which carries the shared world-environment contract
 * (ADR-0011) &mdash; this module never mentions {@code ascension-atmosphere}, and with no
 * atmosphere module installed its planets simply exist without anyone suffocating on them.
 *
 * <p>Planets are <strong>data</strong> (ADR-0004). There is no per-planet Java here and there is
 * not meant to be: adding a world is authoring a JSON file, which is what makes worlds four
 * through seven content work rather than engineering work.
 *
 * <p>Design record: {@code docs/technical/worlds-api.md}.
 */
@Mod(AscensionWorlds.MOD_ID)
public final class AscensionWorlds {

    public static final String MOD_ID = "ascension_worlds";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionWorlds.class);

    public AscensionWorlds(IEventBus modBus, ModContainer container) {
        WorldsContent.register(modBus);
        modBus.addListener(this::onRegisterDataPackRegistries);
        modBus.addListener(this::onCommonSetup);

        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

        LOGGER.info("Ascension Worlds loaded ({})", container.getModInfo().getVersion());
    }

    /**
     * Declare the planet registry.
     *
     * <p>Two codecs: the first reads the JSON, the second syncs entries to clients. Passing the
     * second is what makes planets available client-side, which they have to be &mdash; the
     * client draws a planet in the sky from its position and radius, and under ADR-0010 a planet
     * you cannot see turns space into an empty void with invisible waypoints.
     */
    private void onRegisterDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(WorldsRegistries.PLANET, Planet.CODEC, Planet.CODEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> WorldEnvironmentRegistry.register(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "planets"),
                PlanetEnvironments.INSTANCE));
    }

    /**
     * Read the planet registry into a lookup, before anything can ask about a world.
     *
     * <p>{@code ServerAboutToStart} rather than {@code ServerStarted}: registries are loaded by
     * this point and no player can have joined, so there is no window in which a planet exists
     * and its environment does not.
     */
    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        PlanetEnvironments.load(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        PlanetEnvironments.unload();
    }
}
