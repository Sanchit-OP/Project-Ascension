package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Aetherwright's Siphon minor (docs/technical/rune-system.md section 4.3): you heal for part of
 * the magic damage you deal. A {@link com.ascension.combat.internal.mechanics.MagicDamage.Observer}:
 * told what actually landed, after the target's Magic Resist, so a resisted hit heals less.
 *
 * <p>The heal goes through {@code LivingEntity#heal}, so Grievous Wounds and every other heal rule
 * applies as it does to any healing.
 *
 * <p>Its number is the rune's data: {@code heal}, a share of the damage dealt.
 */
public final class SiphonEffects {

    public static final ResourceLocation ID = Runes.id("siphon");

    private SiphonEffects() {
    }

    public static void onMagicDealt(LivingEntity attacker, LivingEntity target, float amount) {
        if (attacker instanceof Player player) {
            Runes.active(player, ID).ifPresent(rune -> player.heal(amount * (float) rune.value("heal", 0.10)));
        }
    }
}
