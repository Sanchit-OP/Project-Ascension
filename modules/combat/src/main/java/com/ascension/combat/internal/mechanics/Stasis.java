package com.ascension.combat.internal.mechanics;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.net.StasisSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Untargetable, immobile, camera forced to third person (docs/technical/combat-api.md section 5).
 * Guardian's Wing (originally built this) and Zonia's Hourglass are both this exact shape &mdash;
 * enter, hold, exit &mdash; just with different triggers and durations, so the mechanics live here
 * once rather than twice.
 *
 * <p>"Untargetable" is approximated as invulnerable ({@link
 * net.minecraft.world.entity.Entity#setInvulnerable}) rather than literally clearing every nearby
 * mob's target lock &mdash; nothing can land damage either way, and reaching into live mob AI
 * state for the same practical result isn't worth it. Immobile is a {@code -100%} {@link
 * Attributes#MOVEMENT_SPEED} modifier. The camera lock is the one part with no server-side
 * equivalent to check, so it's synced separately via {@link StasisSyncPayload}.
 *
 * <p>Callers own their own duration window (a {@link DurationState}, checked per tick) and decide
 * when to call {@link #apply} and {@link #syncToClient} &mdash; this class only knows how to turn
 * stasis on and off, not when.
 */
public final class Stasis {

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "stasis_immobile");

    private Stasis() {
    }

    public static void apply(ServerPlayer player, boolean active) {
        player.setInvulnerable(active);

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        moveSpeed.removeModifier(MOVE_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        moveSpeed.addTransientModifier(new AttributeModifier(
                MOVE_SPEED_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void syncToClient(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new StasisSyncPayload(active));
    }
}
