package com.ascension.compat.bettercombat;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-bettercombat}.
 *
 * <p>Tier 2 in ADR-0003. The entire integration is data: one Better Combat
 * {@code weapon_attributes} file per ascension-combat melee weapon
 * ({@code data/ascension_combat/weapon_attributes}), choosing how it is held (a two-handed pose or
 * none) and its attack chain from Better Combat's own animations. Every attack keeps a damage
 * multiplier of 1 and no reach bonus, so damage and reach are still the combat module's; Better
 * Combat decides how the swing looks, who in its arc it hits, and the combo rhythm. No Java of
 * Better Combat's is ever called -- this class exists only so NeoForge has a mod entry point to
 * discover the jar's data.
 *
 * <p>{@code required} on both {@code ascension_combat} and {@code bettercombat} in this jar's own
 * {@code neoforge.mods.toml} keeps it from loading into a no-op when either is absent, the same
 * mechanism {@code ascension-compat-endermanoverhaul} uses.
 */
@Mod(AscensionCompatBetterCombat.MOD_ID)
public final class AscensionCompatBetterCombat {

    public static final String MOD_ID = "ascension_compat_bettercombat";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatBetterCombat.class);

    public AscensionCompatBetterCombat() {
        LOGGER.info("Ascension Compat: Better Combat loaded -- combat weapons use its holds and swings");
    }
}
