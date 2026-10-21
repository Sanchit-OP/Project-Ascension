package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Vanguard's Relentless Strikes minor (docs/technical/rune-system.md section 4.1): every hit builds
 * a stack that decays if the next hit doesn't land in time, same shape as Darius's Axe's shred
 * ({@link StackingChargeState}) except the stack lives on the wielder rather than the target, and
 * its payoff is bonus physical damage scaling with the current stack instead of a debuff.
 *
 * <p>No {@code ServerTickEvent} watcher needed the way Darius's Axe needs one for its target-side
 * decay: this stack is only ever read again on the wielder's own next hit, and that hit's handler
 * already checks for a lapsed expiry before refreshing, so there's nothing to clean up in the
 * background between hits.
 *
 * <p>Its numbers are the rune's data: {@code max_stacks}, {@code damage_percent_per_stack},
 * {@code decay_seconds}.
 */
public final class RelentlessStrikesEffects {

    public static final ResourceLocation ID = Runes.id("relentless_strikes");

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

        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = attacker.getData(RuneAttachments.RELENTLESS_STRIKES);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(rune.get().count("max_stacks", 4), now + rune.get().ticks("decay_seconds", 3.0));

        event.setAmount(event.getAmount() * (1.0f + state.stacks() * (float) rune.get().value("damage_percent_per_stack", 0.04)));
    }
}
