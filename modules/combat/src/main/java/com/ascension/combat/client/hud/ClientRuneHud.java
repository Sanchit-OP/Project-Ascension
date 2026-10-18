package com.ascension.combat.client.hud;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.hud.RuneHudEntry;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * This client's copy of what its equipped runes are doing, as the server last said
 * ({@code RuneHudSyncPayload}). Plain values: no player or level held (ADR-0007). Cleared on
 * leaving a world, so a stale chip from another server never shows.
 *
 * <p>Besides the entries it keeps, per entry, how long its cooldown or window is. The server sends
 * only the tick it ends at, so the length is taken as the time left when the entry first arrives,
 * which is the moment it starts.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRuneHud {

    private static List<RuneHudEntry> entries = List.of();
    private static long[] totals = new long[0];
    private static ResourceLocation[] sprites = new ResourceLocation[0];
    private static int[] glyphs = new int[0];

    private ClientRuneHud() {
    }

    public static void update(List<RuneHudEntry> next, long now) {
        long[] nextTotals = new long[next.size()];
        ResourceLocation[] nextSprites = new ResourceLocation[next.size()];
        int[] nextGlyphs = new int[next.size()];
        for (int i = 0; i < next.size(); i++) {
            RuneHudEntry entry = next.get(i);
            nextSprites[i] = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID,
                    "rune/" + entry.rune().getPath());
            nextGlyphs[i] = RuneGlyphs.indexOf(entry.rune().getPath());
            nextTotals[i] = totalFor(entry, now);
        }
        entries = next;
        totals = nextTotals;
        sprites = nextSprites;
        glyphs = nextGlyphs;
    }

    /** Keeps the length already known if the entry is the same one still running; else measures it now. */
    private static long totalFor(RuneHudEntry entry, long now) {
        for (int i = 0; i < entries.size(); i++) {
            RuneHudEntry old = entries.get(i);
            if (old.rune().equals(entry.rune()) && old.kind() == entry.kind() && old.endTick() == entry.endTick()) {
                return totals[i];
            }
        }
        return Math.max(1L, entry.endTick() - now);
    }

    public static void clear() {
        entries = List.of();
        totals = new long[0];
        sprites = new ResourceLocation[0];
        glyphs = new int[0];
    }

    public static int size() {
        return entries.size();
    }

    public static RuneHudEntry entry(int i) {
        return entries.get(i);
    }

    public static long total(int i) {
        return totals[i];
    }

    /** The cell of this chip's smooth glyph, or -1 to use the orrery's pixel one. */
    public static int glyph(int i) {
        return glyphs[i];
    }

    public static ResourceLocation sprite(int i) {
        return sprites[i];
    }
}
