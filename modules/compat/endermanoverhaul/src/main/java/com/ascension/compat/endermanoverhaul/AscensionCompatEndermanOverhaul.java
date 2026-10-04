package com.ascension.compat.endermanoverhaul;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-endermanoverhaul}.
 *
 * <p>Tier 2 in ADR-0003. The entire integration is a single NeoForge {@code add_spawns} biome
 * modifier ({@code data/ascension_compat_endermanoverhaul/neoforge/biome_modifier}) adding
 * Enderman Overhaul's End Islands Enderman to every biome tagged
 * {@code ascension_worlds:moon_biomes}. No Java of Enderman Overhaul's is ever called -- this
 * class exists only so NeoForge has a mod entry point to discover the jar's data.
 *
 * <p>{@code required} on both {@code ascension_worlds} and {@code endermanoverhaul} in this
 * jar's own {@code neoforge.mods.toml} is what keeps it from loading into a no-op when either is
 * absent, the same mechanism {@code ascension-compat-cataclysm} uses for Cataclysm.
 */
@Mod(AscensionCompatEndermanOverhaul.MOD_ID)
public final class AscensionCompatEndermanOverhaul {

    public static final String MOD_ID = "ascension_compat_endermanoverhaul";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatEndermanOverhaul.class);

    public AscensionCompatEndermanOverhaul() {
        LOGGER.info("Ascension Compat: Enderman Overhaul loaded -- End Islands Enderman spawning on the Moon");
    }
}
