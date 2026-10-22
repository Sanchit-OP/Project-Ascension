package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * There are no clouds in Project Ascension, in any dimension (Sanchit, 2026-10-22: the clouds hung over the
 * Moon and the End and spoiled every picture of them). The vanilla cloud setting is held at off, which is what
 * Sodium, Iris and a shader pack all read; Distant Horizons draws its own and has its own switch
 * ({@code ascension-compat-distanthorizons}, and {@code enableCloudRendering} in the pack's config).
 *
 * <p>Held rather than set once because the setting lives in the player's {@code options.txt}, which the pack's
 * default options never overwrite for an existing player, and the video settings screen would otherwise turn it
 * back on. The check is a comparison per client tick.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class NoClouds {

    private NoClouds() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Options options = Minecraft.getInstance().options;
        if (options.cloudStatus().get() != CloudStatus.OFF) {
            options.cloudStatus().set(CloudStatus.OFF);
        }
    }
}
