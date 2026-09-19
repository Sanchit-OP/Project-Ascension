package com.ascension.compat.jade;

import com.ascension.compat.jade.internal.CombatStatsJadePlugin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-jade}.
 *
 * <p>Tier 2 in ADR-0003: this jar exists purely to bridge {@code ascension-combat} and Jade, and
 * simply is not present in a game that does not have both. Client-only ({@code dist = Dist.CLIENT}):
 * a tooltip is a rendered thing, and every stat it reads is already client-synced (both Armor and
 * Magic Resist are {@code setSyncable(true)} attributes), so there is nothing for a dedicated
 * server side of this mod to do at all.
 *
 * <p>{@link CombatStatsJadePlugin} is discovered by Jade's own {@code @WailaPlugin} annotation
 * scan &mdash; nothing here has to register it explicitly, the same way every other Jade addon in
 * the wild works.
 */
@Mod(value = AscensionCompatJade.MOD_ID, dist = Dist.CLIENT)
public final class AscensionCompatJade {

    public static final String MOD_ID = "ascension_compat_jade";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatJade.class);

    public AscensionCompatJade() {
        LOGGER.info("Ascension Compat: Jade loaded");
    }
}
