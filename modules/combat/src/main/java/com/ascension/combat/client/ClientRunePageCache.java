package com.ascension.combat.client;

import com.ascension.combat.internal.net.RunePageSyncPayload;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageRules;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * This client's copy of its saved rune page, kept by {@link ClientNetworkHandlers#onRunePageSync}
 * and read by {@link RuneScreen}: the page, its growing runes' growth, which slots are open, and
 * what a change costs. Server-authoritative; the screen only starts from it.
 *
 * <p>Plain values, no player or level held (ADR-0007). Cleared to empty when the client next
 * receives a sync, so a stale page from another server never shows.
 */
public final class ClientRunePageCache {

    private static RunePage page = RunePage.EMPTY;
    private static Map<ResourceLocation, Integer> growth = Map.of();
    private static RunePageRules.OpenSlots open = RunePageRules.OpenSlots.ALL;
    private static int chalk;
    private static int levels;

    private ClientRunePageCache() {
    }

    public static void update(RunePageSyncPayload payload) {
        page = payload.page();
        growth = Map.copyOf(payload.growth());
        open = payload.open();
        chalk = payload.chalk();
        levels = payload.levels();
    }

    public static RunePage page() {
        return page;
    }

    public static int growth(ResourceLocation rune) {
        return growth.getOrDefault(rune, 0);
    }

    public static RunePageRules.OpenSlots open() {
        return open;
    }

    public static int chalk() {
        return chalk;
    }

    public static int levels() {
        return levels;
    }
}
