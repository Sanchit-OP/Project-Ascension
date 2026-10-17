package com.ascension.combat.internal.rune;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Reading what kind of hit a damage event is, for the runes that care (docs/technical/rune-system.md
 * section 4): fully charged (Cadence, Starfall), critical (Ambush, Starfall). A melee hit's answer
 * is in the swing's {@link SwingMarks}; an arrow's is in the arrow itself, since a fully drawn
 * arrow is a critical one (a crossbow's always are).
 */
public final class RuneHits {

    private RuneHits() {
    }

    /** Whether {@code source} is a fully charged hit by {@code attacker} on {@code victim}. */
    public static boolean charged(DamageSource source, ServerPlayer attacker, LivingEntity victim, long now) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow) {
            return arrow.isCritArrow();
        }

        return direct == attacker && attacker.getData(RuneAttachments.SWING.get()).charged(victim.getUUID(), now);
    }

    /** Whether {@code source} is a critical hit by {@code attacker} on {@code victim}. */
    public static boolean critical(DamageSource source, ServerPlayer attacker, LivingEntity victim, long now) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow) {
            return arrow.isCritArrow();
        }

        return direct == attacker && attacker.getData(RuneAttachments.SWING.get()).crit(victim.getUUID(), now);
    }

    /**
     * Whether {@code source} is a hit with a weapon or an arrow (a melee swing or a shot), by its
     * damage type, so a rune's own lightning or falling star, which are magic, never counts as one.
     */
    public static boolean weaponHit(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() instanceof AbstractArrow;
    }
}
