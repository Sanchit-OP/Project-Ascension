package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
 * <p>The healing half reads {@link LivingDamageEvent.Post} for the same reason every omnivamp-
 * shaped heal in this catalog does (see {@code TastedBloodEffects}): it needs the final damage
 * actually dealt, after every reduction on the target's side, not the pre-mitigation amount.
 */
public final class ConquerorsResolveEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final int MAX_STACKS = 10;

    private static final long STACK_INTERVAL_TICKS = 20L;
    private static final double ATTACK_DAMAGE_PER_STACK = 0.5;
    private static final float HEAL_PERCENT_PER_STACK = 0.01f;

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "conquerors_resolve_attack_damage");

    private ConquerorsResolveEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ConquerorsResolveEffects::onPlayerTick);
        bus.addListener(ConquerorsResolveEffects::onDamagePost);
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
        }
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || !hasKeystone(attacker)) {
            return;
        }

        int stacks = attacker.getData(CombatAttachments.CONQUERORS_RESOLVE_STATE).stacks();
        if (stacks <= 0) {
            return;
        }

        attacker.heal(event.getNewDamage() * HEAL_PERCENT_PER_STACK * stacks);
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
}
