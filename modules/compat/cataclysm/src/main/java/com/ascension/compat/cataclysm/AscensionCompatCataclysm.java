package com.ascension.compat.cataclysm;

import com.ascension.compat.cataclysm.internal.GuardianAwakening;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-cataclysm}.
 *
 * <p>Tier 2 in ADR-0003. Most of the integration is a datapack bundled in
 * {@code src/main/resources/data} that relocates Cataclysm's Ruined Citadel structure onto the
 * Moon at a fixed spot (via {@code ascension-worlds}' own {@code fixed_position} structure
 * placement type) and redirects the Ender Golem/Ender Guardian's loot toward this project's own
 * gateway key. Two mixins (ADR-0013) fix a hardcoded placement height and add a block-swap
 * processor Cataclysm's own JSON has no hook for. {@link GuardianAwakening} spawns the Guardian
 * on the Golem's death, since nothing in Cataclysm's own data or Java spawns it at all.
 *
 * <p>This is the first of what {@code plans/m3-vertical-slice.md} expects to be several
 * {@code ascension-compat-<bossmod>} jars, one per third-party mod a planet's boss is sourced
 * from.
 *
 * <p>{@code required} on both {@code ascension_worlds} and {@code cataclysm} in this jar's own
 * {@code neoforge.mods.toml} is what keeps it from loading into a no-op when either is absent,
 * the same mechanism {@code ascension-compat-chunky} uses for Chunky.
 */
@Mod(AscensionCompatCataclysm.MOD_ID)
public final class AscensionCompatCataclysm {

    public static final String MOD_ID = "ascension_compat_cataclysm";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatCataclysm.class);

    public AscensionCompatCataclysm() {
        GuardianAwakening.register(NeoForge.EVENT_BUS);
        LOGGER.info("Ascension Compat: Cataclysm loaded -- Ruined Citadel relocated to the Moon");
    }
}
