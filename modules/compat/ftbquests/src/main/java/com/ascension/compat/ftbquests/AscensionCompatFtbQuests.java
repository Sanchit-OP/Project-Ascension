package com.ascension.compat.ftbquests;

import com.ascension.compat.ftbquests.client.ChartOverlay;
import com.ascension.compat.ftbquests.client.dev.QuestScreenChecks;
import com.ascension.compat.ftbquests.internal.QuestBookWelcome;
import com.ascension.compat.ftbquests.internal.dev.QuestChecks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-ftbquests}.
 *
 * <p>Tier 2 in ADR-0003, and the lightest kind of it: no compile dependency on FTB Quests, no Ascension
 * module, no mixin. The quest book's whole look is resources (a theme, shapes and pictures) that Quests
 * reads from every pack; this class only adds the Planets page's live "you are here" marker, a texture
 * drawn on the client, a one-time chat line saying where the book is, and a dev-only check, on a server,
 * that the quest files we generate load.
 * The quests themselves are not here: they are data under {@code pack/quests}, turned into Quests' files
 * by {@code tools/generate-quests.py}. See {@code docs/progression/quests.md}.
 */
@Mod(AscensionCompatFtbQuests.MOD_ID)
public final class AscensionCompatFtbQuests {

    public static final String MOD_ID = "ascension_compat_ftbquests";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatFtbQuests.class);

    public AscensionCompatFtbQuests(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(ChartOverlay::registerReloadListener);
            QuestScreenChecks.register(NeoForge.EVENT_BUS);
        }
        QuestChecks.register(NeoForge.EVENT_BUS);
        QuestBookWelcome.register(modBus, NeoForge.EVENT_BUS);
        LOGGER.info("Ascension Compat: FTB Quests loaded");
    }
}
