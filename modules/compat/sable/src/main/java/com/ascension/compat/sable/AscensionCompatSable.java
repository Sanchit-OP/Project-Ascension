package com.ascension.compat.sable;

import com.ascension.compat.sable.internal.SableCommands;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-sable}.
 *
 * <p>Tier 2 (ADR-0003, ADR-0006): bridges {@code ascension-worlds} and Sable, the physics mod under
 * Create Aeronautics, so a ship can cross the boundaries worlds draws between a planet's surface
 * and space. No Tier 1 module knows Sable exists.
 *
 * <p>First increment, 2026-10-11: the transfer itself, behind an op command, to prove in a client
 * that a real Aeronautics ship survives the trip. Ascending by ship comes once it does.
 */
@Mod(AscensionCompatSable.MOD_ID)
public final class AscensionCompatSable {

    public static final String MOD_ID = "ascension_compat_sable";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatSable.class);

    public AscensionCompatSable() {
        SableCommands.register();
        LOGGER.info("Ascension Compat: Sable loaded");
    }
}
