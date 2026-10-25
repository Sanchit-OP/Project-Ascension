package com.ascension.compat.paraglider;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-paraglider}.
 *
 * <p>Tier 2 in ADR-0003. The whole integration is data: Paragliders places three statues by biome tag
 * ({@code paraglider:has_structure/*}), and this jar replaces each of those tags with an empty one, so none is ever placed
 * (Sanchit, 2026-10-25: "let's not even make those structure changes like statues"). Paragliders' village statues, Spirit
 * Orbs, Heart Containers and Stamina Vessels are switched off in the pack's config files, which is the supported way. No Java
 * of Paragliders' is called; this class exists so NeoForge has a mod entry point to discover the jar's data.
 *
 * <p>{@code required} on Paragliders in this jar's own {@code neoforge.mods.toml} keeps it from loading into a no-op without it.
 */
@Mod(AscensionCompatParaglider.MOD_ID)
public final class AscensionCompatParaglider {

    public static final String MOD_ID = "ascension_compat_paraglider";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatParaglider.class);

    public AscensionCompatParaglider() {
        LOGGER.info("Ascension Compat: Paraglider loaded -- no statues in world generation");
    }
}
