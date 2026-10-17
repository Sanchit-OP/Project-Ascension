package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Reaver's Silent Step, the path's <b>world keystone</b> (docs/technical/rune-system.md section
 * 4.2): you make <em>no vibrations</em>, so sculk sensors and shriekers never hear you and the
 * Warden isn't called; and while you sneak, mobs only notice you close up.
 *
 * <p>Vibrations are vanilla game events with you as their cause, and cancelling them at the source
 * is what sculk listens to, so nothing is hunted or faked. Noticing is a mob choosing a target:
 * while you sneak, a mob farther than {@code notice_distance} can't choose you (a mob already
 * hunting you keeps doing so).
 *
 * <p>Its number is the rune's data: {@code notice_distance}, in blocks.
 */
public final class SilentStepEffects {

    public static final ResourceLocation ID = Runes.id("silent_step");

    private SilentStepEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SilentStepEffects::onGameEvent);
        bus.addListener(SilentStepEffects::onChangeTarget);
    }

    private static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player && Runes.active(player, ID).isPresent()) {
            event.setCanceled(true);
        }
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !player.isCrouching()) {
            return;
        }

        LivingEntity mob = event.getEntity();
        if (mob.getLastHurtByMob() == player) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        double notice = rune.get().value("notice_distance", 6.0);
        if (mob.distanceToSqr(player) > notice * notice) {
            event.setCanceled(true);
        }
    }
}
