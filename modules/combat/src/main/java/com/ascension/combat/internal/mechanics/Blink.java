package com.ascension.combat.internal.mechanics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An instant teleport along the wielder's horizontal look direction &mdash; no collision, no
 * travel time (docs/technical/combat-api.md section 5 doesn't name this explicitly, but
 * Wraithblade's active is the shape). Deliberately not the same mechanic as {@link Dash}: Sanchit
 * 2026-09-08 drew the line as blink = instant teleport, nothing in the way matters; dash = real
 * movement that collides with terrain and entities like anything else would. Pitch is ignored so
 * looking up or down doesn't send the wielder into a ceiling or floor; the straight-line raycast
 * is capped at the requested distance or wherever it first hits a solid block, pulled back
 * slightly so the landing spot isn't embedded in a wall.
 */
public final class Blink {

    private static final double WALL_BUFFER = 0.3;

    private Blink() {
    }

    public static void forward(ServerPlayer player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 start = player.position();
        Vec3 desiredEnd = start.add(direction.scale(distance));

        BlockHitResult hit = player.level().clip(new ClipContext(
                start, desiredEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? desiredEnd
                : hit.getLocation().subtract(direction.scale(WALL_BUFFER));

        player.teleportTo(target.x, target.y, target.z);
    }
}
