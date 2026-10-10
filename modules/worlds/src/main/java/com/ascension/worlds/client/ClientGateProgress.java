package com.ascension.worlds.client;

import com.ascension.worlds.internal.GateProgress;
import com.ascension.worlds.internal.net.GateProgressPayload;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Which gates the server last said are lit ({@link GateProgressPayload}), held for the renderers
 * that show it, so none of them asks the server or recounts anything per frame. Ids only: nothing
 * here holds a level or an entity (ADR-0007), and it is emptied on logout so one world's progress
 * never shows in the next.
 */
final class ClientGateProgress {

    private static volatile Set<ResourceLocation> lit = Set.of();

    private ClientGateProgress() {
    }

    static void accept(GateProgressPayload payload) {
        lit = Set.copyOf(payload.litPlanets());
    }

    /** How many glyphs the mark shows right now, 0 to 7 ({@link GateProgress#markStage}). */
    static int markStage() {
        return GateProgress.markStage(lit.size());
    }

    static void clear() {
        lit = Set.of();
    }
}
