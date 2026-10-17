package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.rune.Reentry;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Bulwark's Unbowed minor (docs/technical/rune-system.md section 4.4): harmful effects on you wear
 * off sooner, and knockback moves you less.
 *
 * <p>When a harmful effect lands, its duration is cut by a share: the effect is taken off and put
 * back shorter (a duration can't be changed in place), and only when that application is the one
 * that set the duration, so a source that keeps topping an effect up doesn't have it cut again
 * every time. Knockback is vanilla's own event, scaled down. The Tenacity stat the design
 * asks the stats session for (section 11) would replace both; until it exists the rune does it.
 *
 * <p>Its numbers are the rune's data: {@code effect_reduction} and {@code knockback_reduction}, as
 * fractions.
 */
public final class UnbowedEffects {

    public static final ResourceLocation ID = Runes.id("unbowed");

    /** Guards an effect this rune puts back shorter from being shortened again. */
    private static final Reentry ADJUSTING = new Reentry();

    private UnbowedEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(UnbowedEffects::onEffectAdded);
        bus.addListener(UnbowedEffects::onKnockBack);
    }

    private static void onEffectAdded(MobEffectEvent.Added event) {
        if (ADJUSTING.active() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MobEffectInstance applied = event.getEffectInstance();
        if (applied.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || applied.isInfiniteDuration()) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        MobEffectInstance stored = player.getEffect(applied.getEffect());
        if (rune.isEmpty() || stored == null || stored.getDuration() != applied.getDuration()) {
            return;
        }

        int shorter = shortened(stored.getDuration(), rune.get().value("effect_reduction", 0.30));
        MobEffectInstance replacement = new MobEffectInstance(stored.getEffect(), shorter, stored.getAmplifier(),
                stored.isAmbient(), stored.isVisible(), stored.showIcon());
        // An effect's duration can't be changed in place, so it is taken off and put back shorter.
        ADJUSTING.run(() -> {
            player.removeEffect(stored.getEffect());
            player.addEffect(replacement);
        });
    }

    private static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            event.setStrength(event.getStrength() * (1.0f - (float) rune.get().value("knockback_reduction", 0.40)));
        }
    }

    /** A duration of {@code ticks} cut by the {@code reduction} fraction, never below a tick. */
    static int shortened(int ticks, double reduction) {
        return Math.max(1, (int) Math.round(ticks * (1.0 - reduction)));
    }
}
