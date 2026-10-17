package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Bulwark's Plating minor (docs/technical/rune-system.md section 4.4): after a hard hit, the next
 * few hits are softened for a moment, then it rests. The hit that starts it is not softened: it is
 * what the plating answers.
 *
 * <p>Read late in the damage event ({@link EventPriority#LOW}) so it sees the amount after other
 * bonuses, and softens by a plain share of what is left.
 *
 * <p>Its numbers are the rune's data: {@code trigger_damage}, {@code hits}, {@code window_seconds},
 * {@code reduction} (a fraction), {@code cooldown_seconds}.
 */
public final class PlatingEffects {

    public static final ResourceLocation ID = Runes.id("plating");

    private PlatingEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, PlatingEffects::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        RuneDefinition values = rune.get();
        PlatingState state = player.getData(RuneAttachments.PLATING.get());
        long now = player.level().getGameTime();
        if (state.active(now)) {
            state.consume();
            event.setAmount(event.getAmount() * (1.0f - (float) values.value("reduction", 0.30)));
        } else if (state.ready(now) && event.getAmount() >= values.value("trigger_damage", 4.0)) {
            state.start(now, values.count("hits", 3), values.ticks("window_seconds", 3.0), values.ticks("cooldown_seconds", 12.0));
        }
    }
}
