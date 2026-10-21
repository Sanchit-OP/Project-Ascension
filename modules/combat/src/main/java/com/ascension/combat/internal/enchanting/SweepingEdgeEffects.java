package com.ascension.combat.internal.enchanting;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import com.ascension.combat.internal.mechanics.MagicDamage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Sweeping Edge adds magic damage to every extra target a sweep hits (Sanchit, 2026-10-22: "add something to Sweeping Edge, maybe magic
 * damage with sweeps").
 *
 * <p>Vanilla's Sweeping Edge only scales how hard a sweep hits its neighbours, and Better Combat runs sweeps its own way, so the enchantment
 * did little. Now each extra target, over and above the one the swing was aimed at, takes {@link #MAGIC_PER_LEVEL} plus
 * {@link #MAGIC_PER_LEVEL_PER_AD} of the wielder's Attack Damage, per level, as magic damage that the target's Magic Resist cuts. The target
 * the swing was aimed at gets nothing extra: a sweep is for crowds.
 *
 * <p><b>Telling a sweep's extra target from the aimed one.</b> A swing begins with an {@link AttackEntityEvent} for the target aimed at; every
 * other hit the same player lands in the same game tick (vanilla's sweep, or Better Combat's extra targets) is a sweep hit. So the first
 * target of the tick is remembered per player, as an id and a tick, and a hit on anyone else that tick is a sweep hit. It is the same for
 * vanilla's sweep and for Better Combat's, since neither depends on how the extra targets are reached.
 *
 * <p>Teardown (ADR-0007): the map holds ids and numbers only, and is emptied on logout and on server stop.
 */
public final class SweepingEdgeEffects {

    /** Magic damage a level of Sweeping Edge adds to each extra target. Placeholders to tune by playing. */
    public static final float MAGIC_PER_LEVEL = 3.0f;
    public static final float MAGIC_PER_LEVEL_PER_AD = 0.15f;

    /** The target a player's swing was aimed at, and the game tick it was aimed. */
    private static final class Swing {
        long tick = -1L;
        UUID first;
    }

    private static final Map<UUID, Swing> swings = new HashMap<>();

    private SweepingEdgeEffects() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SweepingEdgeEffects::onAttack);
        gameBus.addListener(SweepingEdgeEffects::onDamagePre);
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> swings.remove(event.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent event) -> swings.clear());
    }

    private static int level(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty() || !weapon.isEnchanted()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), weapon);
    }

    private static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || level(player) <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        Swing swing = swings.computeIfAbsent(player.getUUID(), id -> new Swing());
        if (swing.tick != now) {
            swing.tick = now;
            swing.first = event.getTarget().getUUID();
        }
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        Swing swing = swings.get(player.getUUID());
        if (swing == null || swing.tick != player.level().getGameTime() || event.getEntity().getUUID().equals(swing.first)) {
            return;
        }
        int level = level(player);
        if (level > 0) {
            MagicDamage.apply(event, level * (MAGIC_PER_LEVEL + MAGIC_PER_LEVEL_PER_AD * AbilityScaling.attackDamage(player)));
        }
    }
}
