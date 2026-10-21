package com.ascension.combat.internal.enchanting;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import com.ascension.combat.internal.mechanics.MagicDamage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Two enchantments that are a source of magic damage (Sanchit, 2026-10-22: magic had too few sources for Magic Resist and
 * Magic Penetration to matter).
 *
 * <ul>
 *   <li><b>Arcane Edge</b> (melee weapons): a melee hit adds {@link #BASE_PER_LEVEL} plus {@link #PER_LEVEL_PER_AD} of the
 *       wielder's Attack Damage, per level, as magic damage.</li>
 *   <li><b>Aether Tip</b> (bows and crossbows): an arrow that hits adds the same, the level read from the weapon that fired it.</li>
 * </ul>
 *
 * <p>Both ride the hit that is already landing ({@link MagicDamage#apply}), so the target's invulnerability frames cannot swallow
 * them, Magic Resist and Magic Penetration cut them as for every other magic source, and the Aetherwright boosters (Attunement,
 * Clarity, Gathering Charge, Siphon) see them as magic damage dealt. Spellpiercing is plain data (a Magic Penetration modifier).
 *
 * <p>No state is kept: the level is read from the stack on each hit (ADR-0007).
 */
public final class ArcaneEnchantEffects {

    /** Magic damage a level adds, plus a share of Attack Damage. Placeholders to tune by playing. */
    public static final float BASE_PER_LEVEL = 4.0f;
    public static final float PER_LEVEL_PER_AD = 0.12f;

    private static final ResourceKey<Enchantment> ARCANE_EDGE = key("arcane_edge");
    private static final ResourceKey<Enchantment> AETHER_TIP = key("aether_tip");

    private ArcaneEnchantEffects() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ArcaneEnchantEffects::onDamagePre);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("ascension_combat", path));
    }

    private static int level(ServerPlayer player, ItemStack weapon, ResourceKey<Enchantment> enchantment) {
        if (weapon.isEmpty() || !weapon.isEnchanted()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), weapon);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int level;
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            level = level(player, player.getMainHandItem(), ARCANE_EDGE);
        } else if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
            level = level(player, arrow.getWeaponItem() == null ? ItemStack.EMPTY : arrow.getWeaponItem(), AETHER_TIP);
        } else {
            return;
        }
        if (level > 0) {
            MagicDamage.apply(event, level * (BASE_PER_LEVEL + PER_LEVEL_PER_AD * AbilityScaling.attackDamage(player)));
        }
    }
}
