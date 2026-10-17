package com.ascension.combat.internal.mechanics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A real burst of movement in the wielder's horizontal look direction &mdash; travels over the
 * next several ticks, colliding with terrain and entities like any other movement, rather than
 * skipping past them. Deliberately not the same mechanic as {@link Blink}: Sanchit 2026-09-08
 * drew the line as dash = real movement, blink = instant teleport with nothing in the way.
 *
 * <p>Built on {@code LivingEntity#knockback}, not a hand-rolled velocity/sync system &mdash; it's
 * vanilla's own already-proven way to push a player-controlled entity and have the client actually
 * respect it (server-authoritative velocity pushes on players are otherwise easy to get wrong;
 * knockback already solves that). {@code knockback(strength, x, z)} pushes <em>away</em> from
 * {@code (x, z)}, so the direction passed in here is the wielder's look direction negated.
 */
public final class Dash {

    private Dash() {
    }

    public static void forward(ServerPlayer player, double strength) {
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0, look.z).normalize();
        player.knockback(strength, -direction.x, -direction.z);
        Movements.moved(player);
    }
}
