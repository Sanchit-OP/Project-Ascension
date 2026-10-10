package com.ascension.compat.distanthorizons.internal;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns Distant Horizons' LOD clouds off in any dimension that has no clouds of its own.
 *
 * <p><strong>Why this is needed.</strong> Vanilla's rule for clouds is the dimension's special
 * effects: a NaN cloud height means none, which is how the Nether, the End, the Moon and space all
 * say it. Distant Horizons switches vanilla's clouds off and draws its own, and as of 2.4.5 it
 * decides where by a rule of its own ({@code AbstractDhLevel}): every dimension except one with a
 * ceiling or one named {@code the_end}. So it drew clouds over the Moon and in space.
 *
 * <p><strong>How.</strong> Through DH's public api, never its internals: the cloud switch is an
 * {@code IDhApiConfigValue}, which another mod may override in memory without touching the
 * player's config file, and which DH re-reads every frame. Entering a cloudless dimension sets the
 * override to off; entering one with clouds clears the override, so the player's own setting
 * applies again. Only an override this class set is ever cleared.
 *
 * <p>Checked once per client tick against the last dimension handled, which costs a key
 * comparison. A tick rather than a level event because DH's api is filled in late
 * ({@code DhApi.Delayed}); a tick simply tries again. Holds a {@code ResourceKey}, never a level
 * (ADR-0007), and forgets it on logout.
 */
public final class DimensionClouds {

    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionClouds.class);

    /** The dimension the rule was last applied for, or null to apply it on the next tick. */
    @Nullable
    private static ResourceKey<Level> appliedFor;

    /** Whether DH's cloud override is currently ours. */
    private static boolean overriding;

    private DimensionClouds() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DimensionClouds::onClientTick);
        NeoForge.EVENT_BUS.addListener(DimensionClouds::onLoggingOut);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension().equals(appliedFor)) {
            return;
        }
        IDhApiConfigValue<Boolean> clouds = cloudSetting();
        if (clouds == null) {
            return;
        }
        if (Float.isNaN(level.effects().getCloudHeight())) {
            if (!overriding) {
                overriding = clouds.setValue(false);
                if (!overriding) {
                    LOGGER.warn("Distant Horizons refused the cloud override; its clouds will show in {}",
                            level.dimension().location());
                }
            }
        } else {
            release(clouds);
        }
        appliedFor = level.dimension();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        IDhApiConfigValue<Boolean> clouds = cloudSetting();
        if (clouds != null) {
            release(clouds);
        }
        appliedFor = null;
    }

    private static void release(IDhApiConfigValue<Boolean> clouds) {
        if (overriding) {
            clouds.clearValue();
            overriding = false;
        }
    }

    /** DH's cloud switch, or null until DH has filled in its api. */
    @Nullable
    private static IDhApiConfigValue<Boolean> cloudSetting() {
        IDhApiConfig configs = DhApi.Delayed.configs;
        return configs == null ? null : configs.graphics().genericRendering().cloudRenderingEnabled();
    }
}
