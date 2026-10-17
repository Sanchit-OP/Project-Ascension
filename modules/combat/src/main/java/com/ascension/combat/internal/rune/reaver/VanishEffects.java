package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Reaver's Vanish minor (docs/technical/rune-system.md section 4.2): a kill makes you invisible
 * for a moment, and the mobs nearby lose track of you.
 *
 * <p>Both halves happen on the kill and nowhere else: the invisibility is a plain effect (on a
 * player, effects work), and "lose track" is each nearby mob that was hunting you dropping its
 * target, found once, there and then. A cooldown stops a clump of kills renewing it forever.
 *
 * <p>Its numbers are the rune's data: {@code seconds}, {@code cooldown_seconds}, {@code radius}.
 */
public final class VanishEffects {

    public static final ResourceLocation ID = Runes.id("vanish");

    private VanishEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(VanishEffects::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || event.getEntity() == killer) {
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
        cooldown.activateUntil(now + values.ticks("cooldown_seconds", 10.0));
        killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) values.ticks("seconds", 2.0), 0, false, false, true));
        double radius = values.value("radius", 16.0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, killer.getBoundingBox().inflate(radius), m -> m.getTarget() == killer)) {
            mob.setTarget(null);
        }
        level.sendParticles(ParticleTypes.POOF, killer.getX(), killer.getY(0.5), killer.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
    }
}
