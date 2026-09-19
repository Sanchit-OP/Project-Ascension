package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneCatalog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Vanguard's Momentum minor (docs/technical/combat-api.md section 7): chaining hits quickly builds
 * a stack that scales an on-hit heal, decaying if the next hit doesn't land in time &mdash; same
 * {@link StackingChargeState} shape as {@link RelentlessStrikesEffects}, but read from {@link
 * LivingDamageEvent.Post} instead of the pre-mitigation event, since it needs the final damage
 * actually dealt to size the heal, the same reason every omnivamp-shaped heal in this catalog
 * reads {@code Post} rather than {@code LivingIncomingDamageEvent}.
 */
public final class MomentumEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final int MAX_STACKS = 5;

    private static final float HEAL_PERCENT_PER_STACK = 0.01f;
    private static final long DECAY_TICKS = 40L;

    private MomentumEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(MomentumEffects::onDamagePost);
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (!attacker.getData(CombatAttachments.RUNE_PAGE).hasMinor(RuneCatalog.VANGUARD_MOMENTUM.id())) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = attacker.getData(CombatAttachments.MOMENTUM_STACKS);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(MAX_STACKS, now + DECAY_TICKS);

        attacker.heal(event.getNewDamage() * HEAL_PERCENT_PER_STACK * state.stacks());
    }
}
