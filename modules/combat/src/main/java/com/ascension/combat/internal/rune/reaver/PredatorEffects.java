package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.RuneTargets;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Reaver's Predator keystone, the third on the dial (replacing Silent Step on 2026-10-22; Sanchit: "Reaver is a lethality tree, the
 * old one was useless to a player"): Lethality is damage.
 *
 * <ul>
 *   <li><b>Every point of Lethality adds {@code damage_per_lethality} to the damage of your hits</b> (0.4%, so 60 Lethality is
 *       +24%). Lethality already cuts Armor; Predator makes it count even where there is no Armor left to cut, so a player
 *       who builds it is rewarded and a player with a few points from gear gets a few percent.</li>
 *   <li><b>Every kill of an enemy gives a stack</b> of {@code lethality_per_stack} Lethality for {@code stack_seconds}, up to
 *       {@code max_stacks} (a kill refreshes them all), so clearing a room ramps the next fight.</li>
 * </ul>
 *
 * <p>The stack is the player's own state ({@link StackingChargeState}, ids and numbers only, ADR-0007) and its Lethality a
 * modifier under "Runes"; both end on the player's own tick, which reads no page, so a swapped-out rune cannot leave Lethality on.
 */
public final class PredatorEffects {
    public static final ResourceLocation ID = Runes.id("predator");
    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/predator_lethality");

    private PredatorEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(PredatorEffects::onIncomingDamage);
        bus.addListener(PredatorEffects::onDeath);
        bus.addListener(PredatorEffects::onPlayerTick);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !RuneHits.weaponHit(event.getSource())) {
            return;
        }
        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isPresent()) {
            event.setAmount(event.getAmount() * multiplier(StatReader.value(attacker, CombatStats.LETHALITY),
                    rune.get().value("damage_per_lethality", 0.004)));
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || !RuneTargets.wasFoe(killer, event.getEntity())) {
            return;
        }
        Optional<RuneDefinition> rune = Runes.active(killer, ID);
        if (rune.isEmpty()) {
            return;
        }
        RuneDefinition values = rune.get();
        StackingChargeState state = killer.getData(RuneAttachments.PREDATOR.get());
        long now = killer.level().getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(values.count("max_stacks", 5), now + values.ticks("stack_seconds", 10.0));
        RuneModifiers.setStat(killer, CombatStats.LETHALITY, MODIFIER_ID, state.stacks() * values.value("lethality_per_stack", 4.0));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        StackingChargeState state = player.getData(RuneAttachments.PREDATOR.get());
        if (state.stacks() > 0 && state.expiresAtGameTime() <= player.level().getGameTime()) {
            state.clear();
            RuneModifiers.setStat(player, CombatStats.LETHALITY, MODIFIER_ID, 0.0);
        }
    }

    /** The damage multiplier for {@code lethality} points: one, plus {@code perPoint} for each. */
    static float multiplier(double lethality, double perPoint) {
        return (float) (1.0 + perPoint * Math.max(0.0, lethality));
    }
}
