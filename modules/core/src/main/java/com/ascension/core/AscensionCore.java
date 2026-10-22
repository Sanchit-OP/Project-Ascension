package com.ascension.core;

import com.ascension.core.internal.WorldEnvironments;
import com.ascension.core.internal.client.HudElements;
import com.ascension.core.internal.loot.CoreLoot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_core}.
 *
 * <p>Tier 0 in ADR-0003: shared infrastructure for the other Ascension modules, with no gameplay
 * of its own, and loadable with no other Ascension module present.
 *
 * <p>Since ADR-0011 this module holds the <strong>contracts</strong> that more than one Tier 1
 * module needs to agree about, so that Tier 1 modules never depend on each other. The line is
 * contracts, never behaviour, and the test is that removing every Tier 1 module must leave this
 * one doing nothing observable.
 *
 * <p>Which is why all that is wired up here is a registry freeze and two loot-data types (ADR-0029: a
 * condition and a modifier that the other modules' loot files name, and that do nothing until a file does).
 * It changes no game state, registers no items or blocks, and handles no gameplay event.
 *
 * <p>Nothing here may hold a {@code Level}, {@code Player}, {@code Entity} or
 * {@code BlockEntity} in a static or otherwise long-lived field. See ADR-0007.
 */
@Mod(AscensionCore.MOD_ID)
public final class AscensionCore {

    public static final String MOD_ID = "ascension_core";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCore.class);

    public AscensionCore(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onLoadComplete);
        CoreLoot.register(modBus);
        LOGGER.info("Ascension Core loaded ({})", container.getModInfo().getVersion());
    }

    /**
     * Close the shared registries once every mod has had its chance to register.
     *
     * <p>Deliberately {@code FMLLoadCompleteEvent} and not this module's own common setup. Common
     * setup fires per mod and in parallel, so freezing there would close registration before
     * other mods had registered &mdash; and which ones lost would depend on load order, which is
     * the kind of bug that only ever reproduces on somebody else's machine.
     */
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        WorldEnvironments.get().freeze();
        // The HUD contract (ADR-0024) is client-only; a dedicated server never builds it.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HudElements.get().freeze();
        }
    }
}
