package com.ascension.compat.create;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-create}.
 *
 * <p>Tier 2 in ADR-0003, and unlike every other compat jar so far, it registers nothing: the
 * whole integration is a datapack bundled in {@code src/main/resources/data} that overrides
 * Create's Copper/Netherite Backtank and Diving Helmet recipes to always-false conditions and
 * hides those items from recipe viewers. Create's own "wearable tank of pressurised air that
 * drains while worn" is the exact idea {@code ascension-atmosphere}'s oxygen tank already
 * implements under different rules; shipping both is the confusion this jar exists to remove.
 * Diving Boots are left alone -- they are a plain movement item with no air mechanic.
 *
 * <p>{@code required} on Create in this jar's own {@code neoforge.mods.toml} is what keeps it
 * from loading into a no-op when Create is absent, the same mechanism
 * {@code ascension-compat-chunky} uses for Chunky.
 */
@Mod(AscensionCompatCreate.MOD_ID)
public final class AscensionCompatCreate {

    public static final String MOD_ID = "ascension_compat_create";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatCreate.class);

    public AscensionCompatCreate() {
        LOGGER.info("Ascension Compat: Create loaded -- backtank/diving-helmet recipes disabled");
    }
}
