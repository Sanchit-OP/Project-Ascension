package com.ascension.combat.internal.rune;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Who a rune's area damage may hit (docs/technical/rune-system.md section 4): hostile mobs, and any
 * other mob that is hunting the player (a provoked wolf or golem, not a cow that was merely hit), but never another player, a tamed pet, or whatever the
 * player merely stands near. The runes that burst outward (Starfall, Arc, Brace) share this so a
 * villager or a wolf is never collateral.
 *
 * <p>Found when asked for, on the event that needs them, within a box, never by scanning on a tick
 * (ADR-0007).
 */
public final class RuneTargets {

    private RuneTargets() {
    }

    /** Whether {@code candidate} is fair game for {@code attacker}'s area damage. */
    public static boolean isFoe(LivingEntity attacker, LivingEntity candidate) {
        return candidate.isAlive() && wasFoe(attacker, candidate);
    }

    /**
     * Like {@link #isFoe} without asking whether it is still alive, for the moment it dies: a
     * kill that a rune reacts to must have been an enemy, not a cow.
     */
    public static boolean wasFoe(LivingEntity attacker, LivingEntity candidate) {
        if (candidate == attacker || candidate instanceof Player) {
            return false;
        }

        if (candidate instanceof TamableAnimal pet && pet.isTame()) {
            return false;
        }

        return candidate instanceof Enemy || candidate instanceof Mob mob && mob.getTarget() == attacker;
    }

    /** Every foe of {@code attacker} inside {@code box}. */
    public static List<LivingEntity> foes(ServerLevel level, LivingEntity attacker, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(attacker, e));
    }
}
