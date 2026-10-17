package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reaver's Trophy Hunter minor, a <b>growing rune</b> (docs/technical/rune-system.md sections 4.2
 * and 4.5): Lethality that grows with every new kind of enemy you defeat, up to a cap. Counting is
 * {@link com.ascension.combat.internal.rune.RuneGrowth}; this turns the count into Lethality, as a
 * stat modifier under "Runes", refreshed when the page changes or a kind is counted.
 *
 * <p>Its number is the rune's data: {@code lethality_per_kind}, and {@code growth_cap} on the rune.
 */
public final class TrophyHunterEffects {

    public static final ResourceLocation ID = Runes.id("trophy_hunter");

    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/trophy_hunter");

    private TrophyHunterEffects() {
    }

    public static void refresh(ServerPlayer player) {
        double lethality = Runes.active(player, ID)
                .map(rune -> player.getData(CombatAttachments.RUNE_PAGE).growth(ID) * rune.value("lethality_per_kind", 1.0))
                .orElse(0.0);
        RuneModifiers.setStat(player, CombatStats.LETHALITY, MODIFIER_ID, lethality);
    }
}
