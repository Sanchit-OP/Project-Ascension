package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.rune.GrowingStat;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bulwark's Deep Roots minor, a <b>growing rune</b> (docs/technical/rune-system.md sections 4.4
 * and 4.5): max HP that grows with every new kind of enemy you defeat, up to a cap. League's
 * Overgrowth, Sanchit's own example. Counting is {@link com.ascension.combat.internal.rune.RuneGrowth};
 * this turns the count into Health, as a stat modifier under "Runes", refreshed when the page changes
 * or a kind is counted.
 *
 * <p>Its number is the rune's data: {@code health_per_kind} (0.5 is half a heart's worth), and
 * {@code growth_cap} on the rune.
 */
public final class DeepRootsEffects {

    public static final ResourceLocation ID = Runes.id("deep_roots");

    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/deep_roots");

    private DeepRootsEffects() {
    }

    public static void refresh(ServerPlayer player) {
        GrowingStat.refresh(player, ID, "health_per_kind", 0.5, CombatStats.HEALTH, MODIFIER_ID);
    }
}
