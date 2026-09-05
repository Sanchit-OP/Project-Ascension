package com.ascension.core;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_core}.
 *
 * <p>This module is Tier 0 in the architecture described by ADR-0003: it provides shared
 * infrastructure to other Ascension modules and contains no gameplay of its own. It must
 * remain loadable with no other Ascension module present.
 *
 * <p>Nothing here may hold a reference to a {@code Level}, {@code Player}, {@code Entity} or
 * {@code BlockEntity} in a static or otherwise long-lived field. See ADR-0007.
 */
@Mod(AscensionCore.MOD_ID)
public final class AscensionCore {

    public static final String MOD_ID = "ascension_core";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCore.class);

    public AscensionCore(IEventBus modBus, ModContainer container) {
        LOGGER.info("Ascension Core loaded ({})", container.getModInfo().getVersion());
    }
}
