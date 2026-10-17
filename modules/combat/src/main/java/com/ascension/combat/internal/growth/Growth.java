package com.ascension.combat.internal.growth;

import com.ascension.combat.client.ClientGrowth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Player growth (docs/progression/growth.md, ADR-0021): permanent skill points from milestones,
 * spent in a four-arm tree. This package's one entry point, called once from {@code AscensionCombat},
 * so the shared entry class carries one line for all of it.
 */
public final class Growth {

    private Growth() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        GrowthAttachments.register(modBus);
        modBus.addListener(Growth::onNewDataPackRegistries);
        modBus.addListener(GrowthNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientGrowth.register(gameBus);
        }
        GrowthEvents.register(gameBus);
        gameBus.addListener((RegisterCommandsEvent event) -> GrowthCommands.register(event.getDispatcher()));
    }

    /**
     * Both registries are synced to clients: the tree screen draws the nodes, and lists the
     * milestones a player's points came from.
     */
    private static void onNewDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(GrowthMilestone.REGISTRY, GrowthMilestone.CODEC, GrowthMilestone.CODEC);
        event.dataPackRegistry(GrowthNode.REGISTRY, GrowthNode.CODEC, GrowthNode.CODEC);
    }
}
