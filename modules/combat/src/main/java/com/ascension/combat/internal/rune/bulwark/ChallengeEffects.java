package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Bulwark's Challenge minor (docs/technical/rune-system.md section 4.4): mobs you hit keep attacking
 * you for a while instead of your allies, your pets or the villagers.
 *
 * <p>A hit marks the mob ({@link ChallengeState}: who, and until when) and turns it on you; while
 * the mark lasts, whenever the mob would choose another target the choice is changed back to
 * you. The challenger is a UUID, found again when needed, never held (ADR-0007). Works on bosses
 * too: it changes a decision, it applies no effect.
 *
 * <p>Its number is the rune's data: {@code seconds}.
 */
public final class ChallengeEffects {

    public static final ResourceLocation ID = Runes.id("challenge");

    private ChallengeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ChallengeEffects::onIncomingDamage);
        bus.addListener(ChallengeEffects::onChangeTarget);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }

        mob.getData(RuneAttachments.CHALLENGED.get())
                .challenge(attacker.getUUID(), mob.level().getGameTime() + rune.get().ticks("seconds", 4.0));
        mob.setTarget(attacker);
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.hasData(RuneAttachments.CHALLENGED.get())) {
            return;
        }

        ChallengeState state = mob.getData(RuneAttachments.CHALLENGED.get());
        if (!state.active(mob.level().getGameTime())) {
            return;
        }

        LivingEntity challenger = mob.level().getPlayerByUUID(state.challenger());
        if (challenger != null && challenger.isAlive()) {
            event.setNewAboutToBeSetTarget(challenger);
        }
    }
}
