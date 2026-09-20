package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanguard's keystone (docs/technical/combat-api.md section 7): a stack every second in combat,
 * ramping bonus attack damage and a slice of lifesteal, capping out rather than bursting and
 * resetting &mdash; a deliberate fork of League's Conqueror, and meant to read as the mechanical
 * opposite of whatever Reaver's own keystone turns out to be once that path exists. Reuses {@link
 * CombatStackState}, the per-second-while-in-combat shape The Protean Mask already built; this is
 * its second real consumer, which is what pulled that class out of {@code internal.armor}.
 *
 * <p>Granted automatically the instant the player's {@link
 * com.ascension.combat.internal.rune.RunePageState} reports Vanguard as their primary path &mdash;
 * only one keystone exists per path so far (section 7), so there's no separate pick within the
 * path yet, unlike the path selection itself.
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

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final int MAX_STACKS = 10;

    private static final long STACK_INTERVAL_TICKS = 20L;
    private static final double ATTACK_DAMAGE_PER_STACK = 0.5;
    private static final float HEAL_PERCENT_PER_STACK = 0.01f;

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "conquerors_resolve_attack_damage");
    private static final ResourceLocation OMNIVAMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "conquerors_resolve_omnivamp");

    private ConquerorsResolveEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ConquerorsResolveEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!hasKeystone(player) || !CombatStateRegistry.isInCombat(player)) {
            clearStacks(player);
            return;
        }

        CombatStackState state = player.getData(CombatAttachments.CONQUERORS_RESOLVE_STATE);
        long now = player.level().getGameTime();
        if (state.tryGainStack(now, MAX_STACKS, STACK_INTERVAL_TICKS)) {
            applyModifier(player, state.stacks());
            applyOmnivamp(player, state.stacks());
        }
    }

    private static boolean hasKeystone(ServerPlayer player) {
        return player.getData(CombatAttachments.RUNE_PAGE).hasPrimaryPath(RunePath.VANGUARD);
    }

    private static void clearStacks(ServerPlayer player) {
        CombatStackState state = player.getData(CombatAttachments.CONQUERORS_RESOLVE_STATE);
        if (state.stacks() == 0) {
            return;
        }

        state.clear();
        applyModifier(player, 0);
        applyOmnivamp(player, 0);
    }

    private static void applyModifier(ServerPlayer player, int stacks) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        if (stacks <= 0) {
            return;
        }

        attackDamage.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID, stacks * ATTACK_DAMAGE_PER_STACK, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void applyOmnivamp(ServerPlayer player, int stacks) {
        AttributeInstance omnivamp = player.getAttribute(CombatAttributes.OMNIVAMP);
        if (omnivamp == null) {
            return;
        }

        omnivamp.removeModifier(OMNIVAMP_MODIFIER_ID);
        if (stacks <= 0) {
            return;
        }

        omnivamp.addTransientModifier(new AttributeModifier(
                OMNIVAMP_MODIFIER_ID, stacks * HEAL_PERCENT_PER_STACK, AttributeModifier.Operation.ADD_VALUE));
    }
}
