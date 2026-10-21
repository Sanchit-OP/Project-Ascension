package com.ascension.compat.curios;

import com.ascension.atmosphere.api.AtmosphereRegistry;
import com.ascension.combat.api.WornStacks;
import com.ascension.combat.api.stat.StatSources;
import com.ascension.compat.curios.internal.RelicSlot;
import com.ascension.compat.curios.internal.RelicStats;
import com.ascension.compat.curios.internal.TankSlotCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-curios} (Tier 2, ADR-0003).
 *
 * <p>Adds two Curios slots as data and the small bridges that make them mean something: the tank slot is an
 * {@link com.ascension.atmosphere.api.OxygenSourceCollector}, the relic slot is a {@link WornStacks.Provider}, and
 * a relic's stats are carried over by {@link RelicStats}. Tier 1 knows none of this; the tank still works from the
 * inventory and the relics from the off hand when this jar is absent.
 */
@Mod(AscensionCompatCurios.MOD_ID)
public final class AscensionCompatCurios {

    public static final String MOD_ID = "ascension_compat_curios";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatCurios.class);

    public AscensionCompatCurios(IEventBus modBus) {
        AtmosphereRegistry.register(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "tank_slot"), new TankSlotCollector());
        WornStacks.register(new RelicSlot());
        StatSources.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, RelicStats.ID_PREFIX),
                Component.translatable("stat_source.ascension_compat_curios.relic"));
        NeoForge.EVENT_BUS.addListener(RelicStats::onCurioModifiers);
        LOGGER.info("Ascension Compat: Curios loaded");
    }
}
