package com.ascension.compat.distanthorizons;

import com.ascension.compat.distanthorizons.internal.DimensionClouds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-distanthorizons}.
 *
 * <p>Tier 2 in ADR-0003, and like the Immersive Damage Indicators jar it bridges nothing of ours:
 * it makes a third-party mod respect a vanilla setting. Client-only ({@code dist = Dist.CLIENT}):
 * clouds are a rendered thing, and a dedicated server has nothing to do here.
 */
@Mod(value = AscensionCompatDistantHorizons.MOD_ID, dist = Dist.CLIENT)
public final class AscensionCompatDistantHorizons {

    public static final String MOD_ID = "ascension_compat_distanthorizons";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatDistantHorizons.class);

    public AscensionCompatDistantHorizons() {
        DimensionClouds.register();
        LOGGER.info("Ascension Compat: Distant Horizons loaded");
    }
}
