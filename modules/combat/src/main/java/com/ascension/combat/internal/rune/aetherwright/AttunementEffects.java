package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Aetherwright's Attunement minor (docs/technical/rune-system.md section 4.3): all your magic
 * damage is stronger. A {@link com.ascension.combat.internal.mechanics.MagicDamage.Booster}: the
 * one place magic damage is computed asks it, so a Ragefang proc, a falling star and an item's
 * magic all benefit with no change to any of them.
 *
 * <p>Its number is the rune's data: {@code bonus}, as a fraction.
 */
public final class AttunementEffects {

    public static final ResourceLocation ID = Runes.id("attunement");

    private AttunementEffects() {
    }

    public static float boost(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Player player)) {
            return 1.0f;
        }

        return Runes.active(player, ID).map(rune -> 1.0f + (float) rune.value("bonus", 0.12)).orElse(1.0f);
    }
}
