package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.core.api.CombatStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Protean Mask's identity (docs/technical/combat-api.md section 6): a resistance stack (bonus
 * armor and magic resist) every second while in combat, up to a cap; full stacks double the
 * bonus, and the moment of reaching them carries a small chance to backfire for a chunk of the
 * wearer's own max HP.
 *
 * <p>The first item in the catalog to read {@link CombatStateRegistry} &mdash; equipped-but-not-
 * in-combat and equipped-and-fighting are genuinely different states here, unlike every weapon so
 * far where "wielding it" was the only condition that mattered. Gated on {@link EquipmentSlot#HEAD}
 * rather than the main hand, since this is armor.
 */
public final class ProteanMaskEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final int MAX_STACKS = 8;

    private static final long STACK_INTERVAL_TICKS = 20L;
    private static final double ARMOR_PER_STACK = 1.0;
    private static final double MAGIC_RESIST_PER_STACK = 1.0;
    private static final float FULL_STACK_BACKFIRE_CHANCE = 0.005f;
    private static final float BACKFIRE_MAX_HEALTH_PERCENT = 0.5f;

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "protean_mask_armor");
    private static final ResourceLocation MAGIC_RESIST_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "protean_mask_magic_resist");

    private ProteanMaskEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ProteanMaskEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(CombatContent.PROTEAN_MASK.get())) {
            clearStacks(player);
            return;
        }

        if (!CombatStateRegistry.isInCombat(player)) {
            clearStacks(player);
            return;
        }

        CombatStackState state = player.getData(CombatAttachments.PROTEAN_MASK_STATE);
        long now = player.level().getGameTime();
        if (state.tryGainStack(now, MAX_STACKS, STACK_INTERVAL_TICKS)) {
            applyModifiers(player, state.stacks());
            if (state.stacks() == MAX_STACKS) {
                maybeBackfire(player);
            }
        }
    }

    private static void clearStacks(ServerPlayer player) {
        CombatStackState state = player.getData(CombatAttachments.PROTEAN_MASK_STATE);
        if (state.stacks() == 0) {
            return;
        }

        state.clear();
        applyModifiers(player, 0);
    }

    private static void applyModifiers(ServerPlayer player, int stacks) {
        AttributeInstance armor = player.getAttribute(CombatAttributes.ARMOR);
        AttributeInstance magicResist = player.getAttribute(CombatAttributes.MAGIC_RESIST);

        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
        }
        if (magicResist != null) {
            magicResist.removeModifier(MAGIC_RESIST_MODIFIER_ID);
        }

        if (stacks <= 0) {
            return;
        }

        double multiplier = stacks == MAX_STACKS ? 2.0 : 1.0;
        if (armor != null) {
            armor.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID,
                    stacks * ARMOR_PER_STACK * multiplier,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (magicResist != null) {
            magicResist.addTransientModifier(new AttributeModifier(
                    MAGIC_RESIST_MODIFIER_ID,
                    stacks * MAGIC_RESIST_PER_STACK * multiplier,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void maybeBackfire(ServerPlayer player) {
        if (player.getRandom().nextFloat() >= FULL_STACK_BACKFIRE_CHANCE) {
            return;
        }

        // A price the mask charges, not an attack: Magic Resist does not soften it (it is magic-typed, so
        // the shared defence would otherwise cut it).
        MagicDamage.hurtSettled(player, player.damageSources().magic(), player.getMaxHealth() * BACKFIRE_MAX_HEALTH_PERCENT);
    }
}
