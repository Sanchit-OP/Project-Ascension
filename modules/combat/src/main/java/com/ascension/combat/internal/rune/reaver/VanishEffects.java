package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneTargets;
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
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Reaver's Vanish minor (docs/technical/rune-system.md section 4.2): killing an enemy gives you
 * Minecraft's own Invisibility for a couple of seconds, and until it ends nothing can pick you out
 * as a target.
 *
 * <p>Vanilla's invisibility alone does little: a mob still notices an invisible player within a
 * few blocks, and a mob that just lost its target takes the player again on its next look. So the
 * kill does three things: it gives the real effect, the same one a potion does, with its icon and
 * swirls (the picture), it makes every mob nearby that was
 * hunting you drop its target (found once, there and then), and it opens a window, on the player,
 * in which {@link LivingChangeTargetEvent} refuses to let any mob choose them. A cooldown stops a
 * clump of kills renewing it forever.
 *
 * <p>Only a kill of an enemy counts, not a cow. Its numbers are the rune's data: {@code seconds},
 * {@code cooldown_seconds}, {@code radius}.
 */
public final class VanishEffects {

    public static final ResourceLocation ID = Runes.id("vanish");

    private VanishEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(VanishEffects::onDeath);
        bus.addListener(VanishEffects::onChangeTarget);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || !RuneTargets.wasFoe(killer, event.getEntity())) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(killer, ID);
        if (rune.isEmpty() || !(killer.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        DurationState cooldown = killer.getData(RuneAttachments.VANISH_COOLDOWN.get());
        if (cooldown.isActive(now)) {
            return;
        }

        RuneDefinition values = rune.get();
        long window = values.ticks("seconds", 2.0);
        cooldown.activateUntil(now + values.ticks("cooldown_seconds", 10.0));
        killer.getData(RuneAttachments.VANISH_WINDOW.get()).activateUntil(now + window);
        killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) window));
        double radius = values.value("radius", 16.0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, killer.getBoundingBox().inflate(radius), m -> m.getTarget() == killer)) {
            mob.setTarget(null);
        }
        level.sendParticles(ParticleTypes.POOF, killer.getX(), killer.getY(0.5), killer.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, killer.getX(), killer.getY(), killer.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    /** While a player's window is open, no mob may choose them as a target. */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && player.getExistingData(RuneAttachments.VANISH_WINDOW.get())
                        .map(window -> window.isActive(player.level().getGameTime())).orElse(false)) {
            event.setCanceled(true);
        }
    }
}
