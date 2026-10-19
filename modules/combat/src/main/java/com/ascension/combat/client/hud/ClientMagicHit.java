package com.ascension.combat.client.hud;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The last magic hit this client was told about: when, and how strong. Two numbers, no player or
 * level (ADR-0007); {@link MagicHitLayer} reads it every frame.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMagicHit {

    /** Game time of the last pulse, or {@link Long#MIN_VALUE} when there is none. */
    private static long startedAt = Long.MIN_VALUE;
    private static float strength;

    private ClientMagicHit() {
    }

    /** A magic hit just landed on the player: start (or restart) the tint. */
    public static void pulse(float hitStrength) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        startedAt = mc.level.getGameTime();
        strength = hitStrength;
    }

    /** True when there has been a pulse, however long ago; the layer works out whether it has faded. */
    public static boolean any() {
        return startedAt != Long.MIN_VALUE;
    }

    public static long startedAt() {
        return startedAt;
    }

    public static float strength() {
        return strength;
    }

    /** Forget it, on leaving a world. */
    public static void clear() {
        startedAt = Long.MIN_VALUE;
        strength = 0.0f;
    }
}
