package com.ascension.compat.ftbteams;

import com.ascension.compat.ftbteams.internal.FtbTeamEvents;
import com.ascension.compat.ftbteams.internal.FtbTeamSource;
import com.ascension.compat.ftbteams.internal.FtbTeamWelcome;
import com.ascension.core.api.TeamRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-ftbteams}.
 *
 * <p>Tier 2 in ADR-0003: this jar exists purely to make an FTB Teams party the team Ascension means
 * (ADR-0023), and simply is not present in a game that does not have FTB Teams. It answers
 * {@link TeamRegistry}'s question from FTB Teams' own data, and tells Ascension when a player's team
 * changes. It adds no content, no commands and no screens: creating, inviting and leaving are FTB
 * Teams' own, apart from one chat line, once, that tells a player on their own where the team screen is.
 *
 * <p>Needs only {@code core} from Ascension. {@code ascension-combat} asks the registry and never
 * learns that FTB Teams is the one answering.
 */
@Mod(AscensionCompatFtbTeams.MOD_ID)
public final class AscensionCompatFtbTeams {

    public static final String MOD_ID = "ascension_compat_ftbteams";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatFtbTeams.class);

    public AscensionCompatFtbTeams(IEventBus modBus) {
        TeamRegistry.register(new FtbTeamSource());
        FtbTeamEvents.register();
        FtbTeamWelcome.register(modBus, NeoForge.EVENT_BUS);
        LOGGER.info("Ascension Compat: FTB Teams loaded");
    }
}
