package com.ascension.combat.internal.weapons;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Blink;
import com.ascension.combat.internal.mechanics.DefenseFormula;
import com.ascension.combat.internal.mechanics.DurationState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wraithblade's identity (docs/technical/combat-api.md section 6), revised by Sanchit 2026-09-08
 * after true entity-collision phasing turned out to need a mixin banned in Tier 1 by ADR-0003
 * rule 2 (no NeoForge event exposes vanilla's entity-push, and the one field that suppresses it,
 * {@code Entity#noPhysics}, also disables block collision): passive flat armor penetration
 * (lethality) on every hit, and an active that blinks the wielder forward a short distance, then
 * grants a move-speed boost for a few seconds, on a cooldown.
 *
 * <p>The blink is {@link Blink#forward} &mdash; a one-shot reposition, not continuous collision
 * suppression, so it needs no per-tick bookkeeping of its own; the speed boost that follows it
 * does, via {@link DurationState}.
 *
 * <p><b>Fixed 2026-09-20</b> (Sanchit's own test: a diamond-sword-scale hit was landing for its
 * full raw damage against a Warden with 100 Armor). {@link #onIncomingDamage} used to subtract a
 * flat amount from the already-computed <em>damage-reduction amount</em> &mdash; a small number
 * (a 7-damage hit against 100 Armor reduces by only ~3.5 damage) &mdash; rather than from the
 * target's actual Armor stat, so even the old 4.0 lethality alone was enough to wipe the whole
 * reduction to zero for a light hit. It now recomputes {@link DefenseFormula}'s own curve directly
 * from {@code (target's Armor &minus; lethality)}, the same "effective Armor" reading Sanchit
 * described &mdash; which also means it composes correctly with Darius's Axe's shred, since both
 * read the same live {@link CombatAttributes#ARMOR} value.
 */
public final class WraithbladeEffects {

    /** Raised 4.0 -&gt; 20.0 on 2026-09-20, alongside the fix above -- see this class's own javadoc. */
    private static final float FLAT_LETHALITY = 20.0f;

    private static final double BLINK_DISTANCE = 12.0;
    private static final long SPEED_BOOST_DURATION_TICKS = 60L;
    private static final double MOVE_SPEED_BONUS_PERCENT = 0.30;
    private static final int COOLDOWN_TICKS = 200;

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "wraithblade_move_speed");

    private WraithbladeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(WraithbladeEffects::onIncomingDamage);
        bus.addListener(WraithbladeEffects::onRightClickItem);
        bus.addListener(WraithbladeEffects::onPlayerTick);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.WRAITHBLADE.get())) {
            return;
        }

        LivingEntity target = event.getEntity();
        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) -> {
                    AttributeInstance armor = target.getAttribute(CombatAttributes.ARMOR);
                    if (armor == null) {
                        return reductionIn;
                    }
                    double effectiveArmor = Math.max(0.0, armor.getValue() - FLAT_LETHALITY);
                    return container.getNewDamage() * (float) (1.0 - DefenseFormula.multiplier(effectiveArmor));
                });
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(CombatContent.WRAITHBLADE.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        Blink.forward(player, BLINK_DISTANCE);

        long now = player.level().getGameTime();
        player.getData(CombatAttachments.WRAITHBLADE_SPEED_BOOST)
                .activateUntil(now + SPEED_BOOST_DURATION_TICKS);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState state = player.getData(CombatAttachments.WRAITHBLADE_SPEED_BOOST);
        boolean active = state.isActive(player.level().getGameTime());
        if (!active) {
            state.clear();
        }
        applyMoveSpeedModifier(player, active);
    }

    private static void applyMoveSpeedModifier(Player player, boolean active) {
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        moveSpeed.removeModifier(MOVE_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        moveSpeed.addTransientModifier(new AttributeModifier(
                MOVE_SPEED_MODIFIER_ID,
                MOVE_SPEED_BONUS_PERCENT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
