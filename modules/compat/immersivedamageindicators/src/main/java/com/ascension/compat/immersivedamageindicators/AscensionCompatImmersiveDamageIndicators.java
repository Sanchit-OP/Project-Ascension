package com.ascension.compat.immersivedamageindicators;

import com.ascension.compat.immersivedamageindicators.internal.DamageIndicatorPositioning;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-immersivedamageindicators}.
 *
 * <p>Tier 2 in ADR-0003, and unlike every other compat jar so far, this one bridges nothing of
 * ours &mdash; it exists purely to fix Immersive Damage Indicators' own floating numbers rendering
 * inside a hit target's model instead of above it (Sanchit, 2026-09-19). Client-only ({@code dist
 * = Dist.CLIENT}): the thing being adjusted is a client-rendered particle.
 *
 * <p>Coloring by damage type (physical vs magic) was asked for alongside this but is not built:
 * {@link DamageIndicatorPositioning}'s own javadoc explains why Immersive Damage Indicators has no
 * damage-type information anywhere in its own pipeline to read.
 */
@Mod(value = AscensionCompatImmersiveDamageIndicators.MOD_ID, dist = Dist.CLIENT)
public final class AscensionCompatImmersiveDamageIndicators {

    public static final String MOD_ID = "ascension_compat_immersivedamageindicators";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatImmersiveDamageIndicators.class);

    public AscensionCompatImmersiveDamageIndicators() {
        DamageIndicatorPositioning.register();

        LOGGER.info("Ascension Compat: Immersive Damage Indicators loaded");
    }
}
