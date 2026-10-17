package com.ascension.combat.api.rune;

import com.ascension.combat.internal.rune.reaver.HeadshotMark;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * How another mod tells the runes about a ranged hit it understands better than we do (ADR-0003:
 * integration is announced to us, never required of us).
 *
 * <p>Every projectile that damages an enemy already counts as a ranged hit for Deadeye's distance
 * bonus, whatever mod it comes from. What only the shooting mod knows is where on the body it hit:
 * vanilla arrows are judged by where they land, but a gun's bullets have their own head hitbox.
 * A guns compat jar calls {@link #markHeadshot} from its own "about to hurt" event, in the same
 * tick, just before the damage is dealt.
 */
public final class RangedHits {

    private RangedHits() {
    }

    /**
     * Says the shot that is about to damage {@code victim} hit its head. Only the damage dealt
     * this tick by {@code shooter} to {@code victim} reads it, so a late or stray call does nothing.
     */
    public static void markHeadshot(ServerPlayer shooter, LivingEntity victim) {
        HeadshotMark.mark(shooter, victim);
    }
}
