package com.ascension.compat.jei;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for {@code ascension-compat-jei} (Tier 2, ADR-0003): a "Build path" page in JEI for
 * each combat item. Client-only: JEI is. The page itself is {@code internal.BuildPathPlugin}, which
 * JEI finds through its own {@code @JeiPlugin} annotation scan.
 */
@Mod(value = AscensionCompatJei.MOD_ID, dist = Dist.CLIENT)
public final class AscensionCompatJei {

    public static final String MOD_ID = "ascension_compat_jei";

    public AscensionCompatJei() {
    }
}
