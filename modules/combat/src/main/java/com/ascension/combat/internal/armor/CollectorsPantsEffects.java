package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Collector's Pants' identity (docs/technical/combat-api.md section 6, inspired by The
 * Collector): damage that would leave a target below 5% of its max HP executes outright instead.
 *
 * <p>Registered at {@link EventPriority#LOWEST} so the execute check sees the fully-resolved
 * damage value after every other weapon's own bonuses have already been added to the same
 * {@link LivingDamageEvent.Pre} &mdash; a hit that only crosses the 5% threshold once, say,
 * Ragefang's on-hit magic is added in should still execute, not be judged against the base hit
 * alone.
 *
 * <p><b>The Looting-level bonus is a placeholder translation, not a skipped mechanic.</b> Checked
 * against 1.21.1's actual entity-death loot resolution ({@code LivingEntity#dropFromLootTable}):
 * it builds its {@code LootParams} from {@code Player#getLuck()} (a direct read of {@code
 * Attributes.LUCK}) and never populates a tool-enchantment loot-context parameter for a standard
 * mob kill at all. So "counts as one Looting level higher" is implemented as a one-tick {@code
 * Attributes.LUCK} boost on the exact kill: granted the instant the execute is applied (still
 * present when {@code die()} resolves that kill's loot moments later in the same synchronous call
 * chain, before the wearer's own next tick ever runs) and cleared on the wearer's very next tick.
 * The Luck-per-Looting-level equivalence is a genuine placeholder (combat-api.md section 3), since
 * vanilla defines no such conversion.
 */
public final class CollectorsPantsEffects {

    /** Catalog-specified: below 5% max HP executes. */
    private static final float EXECUTE_THRESHOLD_PERCENT = 0.05f;

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final double EXECUTE_LUCK_BONUS = 1.0;

    private static final ResourceLocation EXECUTE_LUCK_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "collectors_pants_execute_luck");

    private CollectorsPantsEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, CollectorsPantsEffects::onDamagePre);
        bus.addListener(CollectorsPantsEffects::onPlayerTick);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        if (!attacker.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.COLLECTORS_PANTS.get())) {
            return;
        }

        LivingEntity target = event.getEntity();
        float healthAfterHit = target.getHealth() - event.getNewDamage();
        if (healthAfterHit <= 0.0f || healthAfterHit > target.getMaxHealth() * EXECUTE_THRESHOLD_PERCENT) {
            return;
        }

        event.setNewDamage(target.getHealth());
        grantExecuteLuck(attacker);
    }

    private static void grantExecuteLuck(Player attacker) {
        AttributeInstance luck = attacker.getAttribute(Attributes.LUCK);
        if (luck == null) {
            return;
        }

        luck.removeModifier(EXECUTE_LUCK_MODIFIER_ID);
        luck.addTransientModifier(new AttributeModifier(
                EXECUTE_LUCK_MODIFIER_ID, EXECUTE_LUCK_BONUS, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.LEGS).is(CombatContent.COLLECTORS_PANTS.get())) {
            return;
        }

        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck != null) {
            luck.removeModifier(EXECUTE_LUCK_MODIFIER_ID);
        }
    }
}
