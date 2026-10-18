package com.ascension.combat.internal.armor;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import com.ascension.combat.internal.mechanics.Presence;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Shield;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gargoyle Stoneplate's identity (docs/technical/combat-api.md section 6): flat armor and magic
 * resist while worn; the active grants a shield scaling with the number of nearby living
 * entities, on a cooldown. The flat stats are data ({@code item_stats.json}) since 2026-10-16;
 * this class is only the active.
 *
 * <p>Triggered by {@link com.ascension.combat.internal.net.ActivateArmorAbilityPayload}, not a
 * right-click &mdash; see that class and {@link com.ascension.combat.client.CombatKeybinds} for
 * why worn armor needed a new input path. {@link #tryActivate} is the entry point the network
 * handler calls; it re-checks the wearer still has this item equipped rather than trusting the
 * client, since the packet carries no information about which item to activate.
 *
 * <p>The shield reuses vanilla absorption, same as every other Shield/Absorb consumer in this
 * catalog (combat-api.md section 5). Cooldown is tracked against the item itself via {@link
 * net.minecraft.world.item.ItemCooldowns}, the same as a held weapon's active would be, even
 * though this item is never held.
 */
public final class GargoyleStoneplateEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float SHIELD_PER_NEARBY_ENEMY = 4.0f;
    private static final float MAX_SHIELD = 40.0f;
    private static final double SCAN_RADIUS = 12.0;
    private static final int COOLDOWN_TICKS = 400;

    private GargoyleStoneplateEffects() {
    }

    public static void tryActivate(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(CombatContent.GARGOYLE_STONEPLATE.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.GARGOYLE_STONEPLATE.get())) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long nearbyEnemies = level
                .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SCAN_RADIUS))
                .stream()
                .filter(entity -> entity != player && entity.isAlive())
                .count();

        float armor = AbilityScaling.stat(player, com.ascension.combat.api.stat.CombatStats.ARMOR);
        float shield = Math.min(MAX_SHIELD + AbilityScaling.GARGOYLE_CAP_PER_ARMOR * armor,
                nearbyEnemies * (SHIELD_PER_NEARBY_ENEMY + AbilityScaling.GARGOYLE_PER_ENEMY_PER_ARMOR * armor));
        Presence.burst(player, SoundEvents.IRON_GOLEM_REPAIR, 1.0f, 0.7f, ParticleTypes.CRIT, 25, 0.6);
        Shield.grant(player, shield);
        player.getCooldowns().addCooldown(CombatContent.GARGOYLE_STONEPLATE.get(), ItemAbilities.cooldown(player, COOLDOWN_TICKS));
    }
}
