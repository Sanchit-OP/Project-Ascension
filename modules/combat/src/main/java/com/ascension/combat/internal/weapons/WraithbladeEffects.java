package com.ascension.combat.internal.weapons;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.Blink;
import com.ascension.combat.internal.mechanics.DurationState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
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
 * <p><b>Lethality, reworked twice.</b> First fixed 2026-09-20 (Sanchit's own test: a
 * diamond-sword-scale hit was landing for its full raw damage against a Warden with 100 Armor)
 * after discovering its own reduction modifier was subtracting a flat amount from the
 * already-computed <em>damage-reduction amount</em> &mdash; a small number for a light hit &mdash;
 * instead of from the target's Armor stat. Reworked again the same day once {@link
 * CombatAttributes#LETHALITY} became a real attribute: this class no longer touches the damage
 * pipeline at all. Since 2026-10-16 the Lethality is data ({@code item_stats.json}, 20 while held,
 * raised from 4 on 2026-09-20) and this class is only the blink; {@link
 * com.ascension.combat.internal.mechanics.PhysicalDamage} is the one place that actually reads it
 * against a target's Armor now, which is also what lets it compose correctly with Darius's Axe's
 * shred and with Wizard's Agony's percent penetration on the same hit.
 */
public final class WraithbladeEffects {

    private static final double BLINK_DISTANCE = 12.0;
    private static final long SPEED_BOOST_DURATION_TICKS = 60L;
    private static final double MOVE_SPEED_BONUS_PERCENT = 0.30;
    private static final int COOLDOWN_TICKS = 200;

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "wraithblade_move_speed");

    private WraithbladeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(WraithbladeEffects::onRightClickItem);
        bus.addListener(WraithbladeEffects::onPlayerTick);
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
