package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.rune.GrowingStat;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Vanguard's Blooded minor, a <b>growing rune</b> (docs/technical/rune-system.md sections 4.1 and
 * 4.5): omnivamp that grows with every new kind of enemy you defeat, up to a cap. The counting is
 * {@link com.ascension.combat.internal.rune.RuneGrowth}; this class only turns the count into
 * omnivamp.
 *
 * <p>The omnivamp is an attribute modifier, so it is healing through the one shared omnivamp
 * listener like every other source, and it shows on the character sheet under "Runes". Nothing
 * ticks: {@link #refresh} runs when the page is saved, when a kind is counted, and when the player
 * joins or respawns ({@link com.ascension.combat.internal.rune.RuneModifiers}).
 *
 * <p>Its number is the rune's data: {@code omnivamp_per_kind}, written as a percent (0.5 is half a
 * percent), and {@code growth_cap} on the rune itself.
 */
public final class BloodedEffects {

    public static final ResourceLocation ID = Runes.id("blooded");

    /** Under {@code ascension_combat:rune/}, the prefix the stat sheet reads as "Runes". */
    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/blooded");

    private BloodedEffects() {
    }

    /** Sets the player's Blooded omnivamp to what their page and its growth say: none if it isn't on the page. */
    public static void refresh(ServerPlayer player) {
        GrowingStat.refresh(player, ID, "omnivamp_per_kind", 0.5, CombatStats.OMNIVAMP, MODIFIER_ID);
    }
}
