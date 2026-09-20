package com.ascension.combat.internal.weapons;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.MagicDamage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Ragefang's identity (docs/technical/combat-api.md section 6, revised by Sanchit 2026-09-08 to
 * match Guinsoo's Rageblade's actual identity rather than the original "stack then burst" shape):
 *
 * <ul>
 *   <li>Every hit adds 1% attack speed, uncapped in stack count but shadow-capped so the live
 *       {@code Attributes.ATTACK_SPEED} total never exceeds {@link #ATTACK_SPEED_SHADOW_LIMIT}
 *       attacks/sec &mdash; a safety rail against the game bugging out at absurd stack counts,
 *       not a design cap, so it should never need touching for balance.
 *   <li>The stacks don't binary-expire: a hit-free grace period ({@link #DECAY_GRACE_TICKS}),
 *       then a linear ramp down to zero over {@link #DECAY_RAMP_TICKS} more &mdash; four seconds
 *       total from the last hit to fully cold.
 *   <li>Every hit also deals flat bonus magic damage (the real Rageblade identity is on-hit
 *       effects, not a big periodic burst), and every third hit applies that on-hit magic twice
 *       &mdash; physical once, magic twice on the same swing, so it reads as two hits landing at
 *       once.
 * </ul>
 *
 * <p>Same single-event shape as {@link DarkenBladeEffects}: the base hit stays plain physical, and
 * every magic addition goes through {@link MagicDamage} post-armor, so it all lives in one
 * {@link LivingDamageEvent.Pre} listener. The attack-speed decay needs {@link #onPlayerTick} on
 * top of that, since it has to visibly ramp down over several seconds rather than jump straight
 * to zero the next time the wielder happens to land a hit.
 */
public final class RagefangEffects {

    /**
     * Raised 1% -&gt; 5% per hit on 2026-09-20 (Sanchit's own call after testing): at 1%, reaching
     * even a noticeable ramp needed dozens of consecutive hits within the grace window, since
     * {@link #ATTACK_SPEED_SHADOW_LIMIT} only ever bites once the desired percent is huge relative
     * to a diamond-sword-scale base attack speed. The math itself was correct -- {@code
     * Attributes.ATTACK_SPEED} genuinely drives the live swing-cooldown timer -- the increment was
     * just too small to read as "ramping" over a realistic combo length.
     */
    private static final float ATTACK_SPEED_PERCENT_PER_HIT = 0.05f;

    private static final double ATTACK_SPEED_SHADOW_LIMIT = 5.0;
    private static final long DECAY_GRACE_TICKS = 20L;
    private static final long DECAY_RAMP_TICKS = 60L;
    private static final float ON_HIT_MAGIC_DAMAGE = 5.0f;

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "ragefang_attack_speed");

    private RagefangEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RagefangEffects::onDamagePre);
        bus.addListener(RagefangEffects::onPlayerTick);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.RAGEFANG.get())) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RagefangState state = attacker.getData(CombatAttachments.RAGEFANG_STATE);
        long now = serverLevel.getGameTime();
        state.recordHit(now, DECAY_GRACE_TICKS, DECAY_RAMP_TICKS);
        applyAttackSpeedModifier(
                attacker, state.currentAttackSpeedStacks(now, DECAY_GRACE_TICKS, DECAY_RAMP_TICKS));

        int procs = state.everyThirdHit() ? 2 : 1;
        for (int i = 0; i < procs; i++) {
            MagicDamage.apply(event, ON_HIT_MAGIC_DAMAGE);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        RagefangState state = player.getData(CombatAttachments.RAGEFANG_STATE);
        if (state.isAtRest()) {
            return;
        }

        float current = state.currentAttackSpeedStacks(
                player.level().getGameTime(), DECAY_GRACE_TICKS, DECAY_RAMP_TICKS);
        applyAttackSpeedModifier(player, current);
        if (current <= 0.0f) {
            state.clearAttackSpeed();
        }
    }

    private static void applyAttackSpeedModifier(Player player, float stacks) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        if (stacks <= 0.0f) {
            return;
        }

        double baseWithoutOurs = attackSpeed.getValue();
        double desiredPercent = stacks * ATTACK_SPEED_PERCENT_PER_HIT;
        double maxPercent = baseWithoutOurs > 0.0
                ? ATTACK_SPEED_SHADOW_LIMIT / baseWithoutOurs - 1.0
                : 0.0;
        double appliedPercent = Math.min(desiredPercent, Math.max(0.0, maxPercent));
        if (appliedPercent <= 0.0) {
            return;
        }

        attackSpeed.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                appliedPercent,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
