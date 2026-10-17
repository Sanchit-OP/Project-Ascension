package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Aetherwright's Gathering Charge minor, a <b>growing rune</b> (docs/technical/rune-system.md
 * sections 4.3 and 4.5): magic damage that grows with every new kind of enemy you defeat, up to a
 * cap. Counting is {@link com.ascension.combat.internal.rune.RuneGrowth}; this is the {@link
 * com.ascension.combat.internal.mechanics.MagicDamage.Booster} that turns the count into damage.
 *
 * <p>Its number is the rune's data: {@code bonus_per_kind}, and {@code growth_cap} on the rune.
 */
public final class GatheringChargeEffects {

    public static final ResourceLocation ID = Runes.id("gathering_charge");

    private GatheringChargeEffects() {
    }

    public static float boost(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Player player)) {
            return 1.0f;
        }

        return Runes.active(player, ID)
                .map(rune -> 1.0f + (float) (player.getData(CombatAttachments.RUNE_PAGE).growth(ID) * rune.value("bonus_per_kind", 0.01)))
                .orElse(1.0f);
    }
}
