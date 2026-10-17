package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import com.ascension.core.api.CombatStateRegistry;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanguard's keystone (docs/technical/rune-system.md section 4.1): a stack every second in combat,
 * ramping bonus attack damage and a slice of lifesteal, capping out rather than bursting and
 * resetting &mdash; a deliberate fork of League's Conqueror, and meant to read as the mechanical
 * opposite of whatever Reaver's own keystone turns out to be once that path exists. Reuses {@link
 * CombatStackState}, the per-second-while-in-combat shape The Protean Mask already built; this is
 * its second real consumer, which is what pulled that class out of {@code internal.armor}.
 *
 * <p>Active while it is the keystone on the player's page; its numbers are the rune's data
 * ({@code data/ascension_combat/ascension_combat/rune/conquerors_resolve.json}): {@code max_stacks},
 * {@code stack_seconds}, {@code attack_damage_per_stack}, {@code omnivamp_per_stack}.
 *
 * <p><b>The healing half, reworked 2026-09-20.</b> Used to heal the wielder directly in its own
 * {@code LivingDamageEvent.Post} listener. Now that {@link CombatAttributes#OMNIVAMP} is a real
 * attribute, this only keeps that attribute topped up with {@code stacks * HEAL_PERCENT_PER_STACK}
 * whenever the stack count changes; {@link
 * com.ascension.combat.internal.mechanics.OmnivampHealing} is the one place that actually turns it
 * into healing now, the same shared payoff every other Omnivamp source goes through &mdash; this
 * keystone was the one direct-heal holdout left after the item catalog's own pass.
 */
public final class ConquerorsResolveEffects {

    public static final ResourceLocation ID = Runes.id("conquerors_resolve");

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune/conquerors_resolve_attack_damage");
    private static final ResourceLocation OMNIVAMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune/conquerors_resolve_omnivamp");

    private ConquerorsResolveEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ConquerorsResolveEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty() || !CombatStateRegistry.isInCombat(player)) {
            clearStacks(player);
            return;
        }

        RuneDefinition values = rune.get();
        CombatStackState state = player.getData(RuneAttachments.CONQUERORS_RESOLVE);
        long now = player.level().getGameTime();
        if (state.tryGainStack(now, values.count("max_stacks", 10), values.ticks("stack_seconds", 1.0))) {
            applyModifier(player, state.stacks() * values.value("attack_damage_per_stack", 0.5));
            applyOmnivamp(player, state.stacks() * values.value("omnivamp_per_stack", 0.01));
        }
    }

    private static void clearStacks(ServerPlayer player) {
        CombatStackState state = player.getData(RuneAttachments.CONQUERORS_RESOLVE);
        if (state.stacks() == 0) {
            return;
        }

        state.clear();
        applyModifier(player, 0);
        applyOmnivamp(player, 0);
    }

    private static void applyModifier(ServerPlayer player, double amount) {
        RuneModifiers.set(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER_ID, amount,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyOmnivamp(ServerPlayer player, double amount) {
        RuneModifiers.set(player, CombatAttributes.OMNIVAMP, OMNIVAMP_MODIFIER_ID, amount,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
