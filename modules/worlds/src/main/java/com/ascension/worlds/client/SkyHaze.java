package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a dimension's {@link SkyBody.Haze} from its {@code ascension_sky} file: how far its air
 * lets you see, and in what colour. Built for the Nether opened to the sky (ADR-0017).
 *
 * <p>Low priority, and listeners do not receive cancelled events, so a planet weather event (the
 * Moon's dust storm, {@link PlanetWeatherRenderer}) that already set the fog keeps it. Leaves the
 * fog alone under water, in lava or snow, and while blinded or in darkness, where vanilla's own
 * fog is the point.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class SkyHaze {

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) {
            return;
        }
        SkyBody.Haze haze = current(event);
        if (haze == null) {
            return;
        }
        float renderDistance = event.getRenderer().getRenderDistance();
        event.setNearPlaneDistance(Math.min(haze.start(), renderDistance));
        event.setFarPlaneDistance(Math.min(haze.end(), renderDistance));
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        SkyBody.Haze haze = current(event);
        if (haze == null || haze.colour().isEmpty() || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        int colour = haze.colour().get();
        event.setRed(((colour >> 16) & 0xFF) / 255.0F);
        event.setGreen(((colour >> 8) & 0xFF) / 255.0F);
        event.setBlue((colour & 0xFF) / 255.0F);
    }

    /** The haze where the camera is, or null if it has none or vanilla's own fog should win. */
    @Nullable
    private static SkyBody.Haze current(ViewportEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (event.getCamera().getEntity() instanceof LivingEntity viewer
                && (viewer.hasEffect(MobEffects.BLINDNESS) || viewer.hasEffect(MobEffects.DARKNESS))) {
            return null;
        }
        return SkyBodies.hazeFor(level.dimension().location());
    }

    private SkyHaze() {
    }
}
