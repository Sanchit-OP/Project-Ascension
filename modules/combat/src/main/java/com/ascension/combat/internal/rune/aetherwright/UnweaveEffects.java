package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aetherwright's Unweave minor (docs/technical/rune-system.md section 4.3): your magic damage
 * ignores part of the target's Magic Resist. That is exactly what the Magic Penetration stat is,
 * so Unweave is a stat modifier under "Runes", set when the page changes; the shared magic-damage
 * code already reads it.
 *
 * <p>Its number is the rune's data: {@code penetration}, written as a percent (15 for 15%).
 */
public final class UnweaveEffects {

    public static final ResourceLocation ID = Runes.id("unweave");

    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/unweave");

    private UnweaveEffects() {
    }

    public static void refresh(ServerPlayer player) {
        double written = Runes.active(player, ID).map(rune -> rune.value("penetration", 15.0)).orElse(0.0);
        RuneModifiers.setStat(player, CombatStats.MAGIC_PENETRATION, MODIFIER_ID, written);
    }
}
