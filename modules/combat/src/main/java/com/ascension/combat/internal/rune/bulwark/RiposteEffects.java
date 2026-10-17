package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * Bulwark's Riposte minor (docs/technical/rune-system.md section 4.4): raise your shield just as a
 * hit lands (a <b>parry</b>) and the attacker is stunned and your next hit is a critical.
 *
 * <p>A parry is a block made within a moment of raising the shield ({@code
 * LivingEntity#getTicksUsingItem}). The stun is a heavy slow, so it works on what takes effects:
 * the Wither and Cataclysm's bosses refuse effects and are not stunned (the crit still comes). The
 * crit is a short window that the player's next critical-hit check spends; it is read before
 * Pounce so a Pounce multiplier still applies to it.
 *
 * <p>Its numbers are the rune's data: {@code parry_seconds}, {@code stun_seconds},
 * {@code crit_seconds}.
 */
public final class RiposteEffects {

    public static final ResourceLocation ID = Runes.id("riposte");

    private static final float CRIT_MULTIPLIER = 1.5f;

    private RiposteEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RiposteEffects::onShieldBlock);
        bus.addListener(EventPriority.HIGH, RiposteEffects::onCriticalHit);
    }

    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty() || !isParry(player.getTicksUsingItem(), rune.get().ticks("parry_seconds", 0.5))) {
            return;
        }

        RuneDefinition values = rune.get();
        player.getData(RuneAttachments.RIPOSTE.get()).activateUntil(level.getGameTime() + values.ticks("crit_seconds", 2.0));
        if (event.getDamageSource().getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) values.ticks("stun_seconds", 1.0), 6, false, true));
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(1.0), player.getZ(), 14, 0.4, 0.4, 0.4, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    private static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState window = player.getData(RuneAttachments.RIPOSTE.get());
        if (!window.isActive(player.level().getGameTime())) {
            return;
        }

        window.clear();
        event.setCriticalHit(true);
        event.setDamageMultiplier(Math.max(event.getDamageMultiplier(), CRIT_MULTIPLIER));
    }

    /** Whether a shield held for {@code ticksUsing} ticks was raised only just in time to parry. */
    static boolean isParry(int ticksUsing, long parryTicks) {
        return ticksUsing <= parryTicks;
    }
}
