package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Vanguard's Momentum minor (docs/technical/rune-system.md section 4.1): chaining hits quickly builds
 * a stack that scales an on-hit heal, decaying if the next hit doesn't land in time &mdash; same
 * {@link StackingChargeState} shape as {@link RelentlessStrikesEffects}, but read from {@link
 * LivingDamageEvent.Post} instead of the pre-mitigation event, since it needs the final damage
 * actually dealt to size the heal, the same reason every omnivamp-shaped heal in this catalog
 * reads {@code Post} rather than {@code LivingIncomingDamageEvent}.
 *
 * <p>Its numbers are the rune's data: {@code max_stacks}, {@code heal_per_stack} (a fraction of
 * the damage dealt), {@code chain_seconds}.
 */
public final class MomentumEffects {

    public static final ResourceLocation ID = Runes.id("momentum");

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

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = attacker.getData(RuneAttachments.MOMENTUM);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(rune.get().count("max_stacks", 5), now + rune.get().ticks("chain_seconds", 2.0));

        attacker.heal(event.getNewDamage() * (float) rune.get().value("heal_per_stack", 0.01) * state.stacks());
    }
}
