package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneCatalog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Vanguard's Relentless Strikes minor (docs/technical/combat-api.md section 7): every hit builds
 * a stack that decays if the next hit doesn't land in time, same shape as Darius's Axe's shred
 * ({@link StackingChargeState}) except the stack lives on the wielder rather than the target, and
 * its payoff is bonus physical damage scaling with the current stack instead of a debuff.
 *
 * <p>No {@code ServerTickEvent} watcher needed the way Darius's Axe needs one for its target-side
 * decay: this stack is only ever read again on the wielder's own next hit, and that hit's handler
 * already checks for a lapsed expiry before refreshing, so there's nothing to clean up in the
 * background between hits.
 */
public final class RelentlessStrikesEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final int MAX_STACKS = 4;

    private static final float BONUS_DAMAGE_PER_STACK = 0.75f;
    private static final long DECAY_TICKS = 60L;

    private RelentlessStrikesEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RelentlessStrikesEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getData(CombatAttachments.RUNE_PAGE).hasMinor(RuneCatalog.VANGUARD_RELENTLESS_STRIKES.id())) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = attacker.getData(CombatAttachments.RELENTLESS_STRIKES_STACKS);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(MAX_STACKS, now + DECAY_TICKS);

        event.setAmount(event.getAmount() + state.stacks() * BONUS_DAMAGE_PER_STACK);
    }
}
