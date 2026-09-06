package com.ascension.compat.chunky;

import com.ascension.compat.chunky.internal.ChunkyAttachments;
import com.ascension.compat.chunky.internal.ChunkySequentialPregen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension-compat-chunky}.
 *
 * <p>Tier 2 in ADR-0003: this jar exists purely to bridge {@code ascension-worlds} and Chunky,
 * and simply is not present in a game that does not have both. See
 * {@link ChunkySequentialPregen}'s javadoc for how that absence is enforced.
 *
 * <p>Design record: {@code plans/m2-worlds.md}'s M2.6 section, lever #2.
 */
@Mod(AscensionCompatChunky.MOD_ID)
public final class AscensionCompatChunky {

    public static final String MOD_ID = "ascension_compat_chunky";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCompatChunky.class);

    public AscensionCompatChunky(IEventBus modBus) {
        ChunkyAttachments.register(modBus);
        ChunkySequentialPregen.register();

        LOGGER.info("Ascension Compat: Chunky loaded");
    }
}
