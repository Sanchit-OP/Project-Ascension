package com.ascension.compat.sable;

import com.ascension.compat.sable.internal.SableCommands;
import com.ascension.compat.sable.internal.ShipVoyages;
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
 * <p>The airship road to space (ADR-0015): a ship climbing past a planet's build height is carried
 * into space with its crew, and one reaching a planet in space is carried down to it
 * ({@link ShipVoyages}). Space has no gravity for ships (this jar's {@code dimension_physics} data),
 * and new ships get storage slots free in every dimension so they can always cross (ADR-0016).
 * {@code /ascension sable carry} does the same move on demand, for testing.
 */
@Mod(AscensionCompatSable.MOD_ID)
public final class AscensionCompatSable {

    public static final String MOD_ID = "ascension_compat_sable";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatSable.class);

    public AscensionCompatSable() {
        ShipVoyages.register();
        SableCommands.register();
        LOGGER.info("Ascension Compat: Sable loaded");
    }
}
