package com.ascension.combat.internal.rune;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.internal.CombatAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * What every growing rune that gives a stat does with its count (docs/technical/rune-system.md
 * section 4.5): the kinds of enemy counted so far, times a number per kind, as a stat modifier
 * under "Runes". Blooded (omnivamp), Trophy Hunter (Lethality) and Deep Roots (Health) are each one
 * call, and a datapack's growing rune on any stat would be too.
 *
 * <p>Counting is {@link RuneGrowth}; this is the half that turns the count into a stat, run from
 * a rune's refresher when the page changes or a kind is counted.
 */
public final class GrowingStat {

    private GrowingStat() {
    }

    /**
     * Puts {@code kinds counted * the rune's perKindKey value} on {@code player} as {@code stat},
     * written the way a player says it (0.5 is half a percent omnivamp); none when the rune is not
     * on their page.
     *
     * @param rune        the growing rune, whose id is also its effect
     * @param perKindKey  which of the rune's values is the gain per kind
     * @param fallback    the per-kind gain if the file leaves the value out
     * @param modifierId  the modifier's id, under {@code ascension_combat:rune/}
     */
    public static void refresh(ServerPlayer player, ResourceLocation rune, String perKindKey, double fallback, CombatStat stat,
            ResourceLocation modifierId) {
        double written = Runes.active(player, rune)
                .map(definition -> player.getData(CombatAttachments.RUNE_PAGE).growth(rune) * definition.value(perKindKey, fallback))
                .orElse(0.0);
        RuneModifiers.setStat(player, stat, modifierId, written);
    }
}
