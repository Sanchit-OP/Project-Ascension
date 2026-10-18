package com.ascension.compat.create;

import com.ascension.compat.create.internal.CreateContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-create}.
 *
 * <p>Tier 2 in ADR-0003. Two parts:
 * <ul>
 *   <li>A datapack in {@code src/main/resources/data} that overrides Create's Copper/Netherite
 *       Backtank and Diving Helmet recipes to always-false conditions and hides those items from
 *       recipe viewers (our oxygen tank already is that idea), and makes raw helium a
 *       superheated blaze-burner fuel.</li>
 *   <li>The Air Compressor (ADR-0026): a Create kinetic machine, the only source of air in the
 *       pack. It fills the standard air fluid of {@code ascension-atmosphere}'s api, so Create's
 *       tanks, pumps and pipes carry the air to an Air Dispenser with no Create code in Tier 1.</li>
 * </ul>
 *
 * <p>{@code required} on Create in this jar's own {@code neoforge.mods.toml} is what keeps it
 * from loading into a no-op when Create is absent, the same mechanism
 * {@code ascension-compat-chunky} uses for Chunky.
 */
@Mod(AscensionCompatCreate.MOD_ID)
public final class AscensionCompatCreate {

    public static final String MOD_ID = "ascension_compat_create";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatCreate.class);

    public AscensionCompatCreate(IEventBus modBus) {
        CreateContent.register(modBus);
        LOGGER.info("Ascension Compat: Create loaded -- Air Compressor added, backtank/diving-helmet recipes disabled");
    }
}
