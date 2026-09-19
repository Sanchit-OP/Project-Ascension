package com.ascension.compat.immersivedamageindicators.internal;

import toni.immersivedamageindicators.api.CreateDamageIndicator;
import toni.immersivemessages.api.OnPoseMessage;

/**
 * Fixes Immersive Damage Indicators' own floating numbers rendering inside a hit target's model
 * (Sanchit, 2026-09-19) by enlarging every indicator and nudging it up, via the one extension
 * point that mod exposes ({@code CreateDamageIndicator.EVENT}, a Fabric-style event fired the
 * instant a number is about to display &mdash; not a NeoForge {@code IEventBus} event, since this
 * mod is built on Forgified Fabric API).
 *
 * <p>Two earlier approaches were tried and confirmed (by reading {@code DamageParticle} and
 * {@code ImmersiveMessage}'s real source on GitHub, {@code txnimc/ImmersiveDamageIndicators} and
 * {@code txnimc/ImmersiveMessages}) to be structurally incapable of working, despite this
 * listener firing correctly every hit:
 * <ul>
 *   <li>Mutating {@code message.yLevel} directly does nothing &mdash; {@code DamageParticle} is a
 *       real 3D world-space {@code Particle}, not a screen-space overlay. Its render position
 *       comes from the particle's own {@code x/y/z} plus a built-in {@code visualDY} drift; the
 *       {@code ImmersiveMessage}'s {@code yLevel}/{@code xLevel} fields are only meaningful for
 *       that library's screen-anchored toast/popup usage, and are never read anywhere in
 *       {@code DamageParticle}'s render path.</li>
 *   <li>Calling {@code message.size(float)} does nothing either &mdash; it only sets a default
 *       used when no animation is registered for {@code Binding.Size}, but {@code DamageParticle}'s
 *       own constructor already registers two {@code Binding.Size} transitions covering this
 *       particle's entire visible lifetime (a crit-dependent pop from 0 up to 2-3x, easing back
 *       down to 1-1.5x). Those transitions are resolved every frame via
 *       {@code AnimationTimeline.getKeyframe()} and completely dominate over any static default.</li>
 * </ul>
 *
 * <p>What actually works: {@code DamageParticle}'s constructor sets
 * {@code message.onPoseMessage = this::applyPose} (its own scale/rotation callback) <em>before</em>
 * firing {@code CreateDamageIndicator.EVENT} — and that field is public and freely reassignable.
 * This wraps the original callback rather than replacing it, so the built-in crit pop/shake
 * animation is preserved; it just adds a further scale-around-center and an upward translate on
 * top, in the same pixel-like local units {@code applyPose} already operates in (the outer
 * per-particle distance scale is applied by {@code DamageParticle.render} *after* this callback
 * returns, so an offset set here scales with distance the same way font size already does).
 *
 * <p>{@code Y_OFFSET} and {@code SIZE_MULTIPLIER} are visual placeholders, not balance numbers
 * &mdash; this project has no established way to "test" a render offset other than looking at it
 * in game, so treat these the same way any other placeholder in this codebase is treated: adjust
 * and redeploy until it looks right, rather than as a value trusted to be correct on the first try.
 *
 * <p><strong>Coloring by damage type (physical vs magic) is not done here</strong>, despite being
 * asked for alongside the position fix: {@code DamageParticle} carries only an already-built
 * {@code ImmersiveMessage}, and Immersive Damage Indicators' own {@code LivingEntityMixin} detects
 * a hit purely from a per-tick health delta observed client-side (confirmed by decompiling it) --
 * there is no damage source, damage type, or even distinct amount-per-hit information anywhere in
 * its pipeline for this class to read. Telling a magic hit from a physical one here would need a
 * new sync channel of this project's own (a very short-lived "was the last hit on this target
 * magic" flag, pushed to the client and read at the moment this event fires) and would still be
 * wrong the instant two differently-typed hits land on the same target in the same tick, since
 * Immersive Damage Indicators only ever shows one combined number for a tick's total damage taken.
 */
public final class DamageIndicatorPositioning {

    private static final float Y_OFFSET = 24.0f;
    private static final float SIZE_MULTIPLIER = 1.6f;

    private DamageIndicatorPositioning() {
    }

    public static void register() {
        CreateDamageIndicator.EVENT.register((particle, message) -> {
            OnPoseMessage original = message.onPoseMessage;
            message.onPoseMessage = (msg, animation, context, bgOffset, anchor, align, objectWidth, objectHeight) -> {
                var key = original != null
                        ? original.applyPose(msg, animation, context, bgOffset, anchor, align, objectWidth, objectHeight)
                        : animation.getKeyframe();

                context.pose().translate(0f, -Y_OFFSET, 0f);
                context.pose().translate(objectWidth / 2f, objectHeight / 2f, 0f);
                context.pose().scale(SIZE_MULTIPLIER, SIZE_MULTIPLIER, SIZE_MULTIPLIER);
                context.pose().translate(objectWidth / -2f, objectHeight / -2f, 0f);

                return key;
            };
        });
    }
}
