package com.ascension.hud;

import com.ascension.hud.internal.HudConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_hud} (ADR-0022).
 *
 * <p>Owns the layout, theme and frame of the HUD. What is drawn comes from elements that other
 * modules register through {@code HudRegistry} in {@code ascension_core}; this module never
 * depends on them, so each works with only core present (ADR-0003 rule 5).
 *
 * <p>Everything that touches the screen lives under {@code client}, which a dedicated server
 * never loads. This class stays free of client types for that reason.
 */
@Mod(AscensionHud.MOD_ID)
public final class AscensionHud {

    public static final String MOD_ID = "ascension_hud";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionHud.class);

    public AscensionHud(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
        LOGGER.info("Ascension HUD loaded ({})", container.getModInfo().getVersion());
    }
}
