package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.CombatContent;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Collector's Pants' identity (docs/technical/combat-api.md section 6, inspired by The
 * Collector): damage that would leave a target below 5% of its max HP executes outright instead.
 *
 * <p>Registered at {@link EventPriority#LOWEST} so the execute check sees the fully-resolved
 * damage value after every other weapon's own bonuses have already been added to the same
 * {@link LivingDamageEvent.Pre} &mdash; a hit that only crosses the 5% threshold once, say,
 * Ragefang's on-hit magic is added in should still execute, not be judged against the base hit
 * alone.
 *
 * <p><b>Looting.</b> While worn, the wearer's weapon counts as one Looting level higher on every kill: a Looting III sword acts as IV
 * (Sanchit, 2026-10-22). Vanilla 1.21.1 reads the level from the killer's equipment when a mob's loot table runs ({@code
 * enchanted_count_increase}, {@code random_chance_with_enchanted_bonus}), so the bonus is added there, virtually: the kill opens a window at
 * {@link LivingDeathEvent} (the moment before the loot is rolled) for the killer's main-hand stack, NeoForge's {@link
 * GetEnchantmentLevelEvent} adds one level to that stack's Looting while the window is open, and {@link LivingDropsEvent} (the end of the
 * same call) closes it. Nothing on the item changes. (The first version of this class boosted Luck instead, which loot tables of this
 * version do not read for Looting, so it did nothing.) The one piece of state is the window: a flag and the identity of one stack, no
 * player or world.
 */
public final class CollectorsPantsEffects {

    /** Catalog-specified: below 5% max HP executes. */
    private static final float EXECUTE_THRESHOLD_PERCENT = 0.05f;

    /** Levels of Looting the pants add on top of the weapon's own. */
    private static final int EXTRA_LOOTING_LEVELS = 1;

    /** True while a wearer's kill is rolling its loot; {@link #windowStack} is the killer's main-hand stack then. */
    private static boolean windowOpen;
    private static int windowStack;

    private CollectorsPantsEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, CollectorsPantsEffects::onDamagePre);
        bus.addListener(CollectorsPantsEffects::onDeath);
        bus.addListener(CollectorsPantsEffects::onEnchantmentLevel);
        bus.addListener(EventPriority.LOWEST, CollectorsPantsEffects::onDrops);
        bus.addListener(CollectorsPantsEffects::onPlayerTick);
        bus.addListener((ServerStoppedEvent event) -> windowOpen = false);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.COLLECTORS_PANTS.get())) {
            return;
        }

        LivingEntity target = event.getEntity();
        float healthAfterHit = target.getHealth() - event.getNewDamage();
        if (healthAfterHit <= 0.0f || healthAfterHit > target.getMaxHealth() * EXECUTE_THRESHOLD_PERCENT) {
            return;
        }

        event.setNewDamage(target.getHealth());
    }

    /** A wearer's kill is about to roll its loot: open the window for the weapon in their main hand. */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)
                || !killer.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.COLLECTORS_PANTS.get())) {
            return;
        }
        ItemStack weapon = killer.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }
        windowOpen = true;
        windowStack = System.identityHashCode(weapon);
    }

    /** While the window is open, the killer's weapon reads one Looting level higher. */
    private static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (!windowOpen || System.identityHashCode(event.getStack()) != windowStack) {
            return;
        }
        Holder<Enchantment> looting = event.getLookup().getOrThrow(Enchantments.LOOTING);
        event.getEnchantments().set(looting, event.getEnchantments().getLevel(looting) + EXTRA_LOOTING_LEVELS);
    }

    /** The loot has been rolled. */
    private static void onDrops(LivingDropsEvent event) {
        windowOpen = false;
    }

    /** A safety: a window that somehow outlived its kill closes on the next player tick. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        windowOpen = false;
    }
}
