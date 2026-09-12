package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.net.StasisSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Guardian's Wing's identity (docs/technical/combat-api.md section 6): on death, while off
 * cooldown, stasis (section 5) instead of dying, then revive at half HP.
 *
 * <p>Stasis here is invulnerable ({@link net.minecraft.world.entity.Entity#setInvulnerable}),
 * immobile (a {@code -100%} {@link Attributes#MOVEMENT_SPEED} modifier), and the client's camera
 * forced to third person via {@link StasisSyncPayload}. "Untargetable" is approximated as
 * invulnerable rather than literally clearing every nearby mob's target lock &mdash; nothing can
 * land damage either way, and reaching into live mob AI state for the same practical result isn't
 * worth it.
 *
 * <p>Reuses {@link DurationState} for the stasis window, same shape as every other "active until
 * this game tick" state in this catalog. {@link DurationState#hasExpired}, added for this item, is
 * what lets {@link #onPlayerTick} do the revive exactly once, on the tick stasis lapses, rather
 * than on every tick after.
 *
 * <p>Preventing the actual death is a cancelled {@link LivingDeathEvent} plus an immediate {@code
 * setHealth(1)}, the same shape most "prevent death" effects use across the ecosystem &mdash; the
 * event's own contract is that a cancelled death simply doesn't happen, so nothing else needs to
 * be undone.
 */
public final class GuardianWingEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final long STASIS_DURATION_TICKS = 80L;

    private static final float REVIVE_HEALTH_PERCENT = 0.5f;
    private static final int COOLDOWN_TICKS = 6000;

    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "guardian_wing_stasis_immobile");

    private GuardianWingEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(GuardianWingEffects::onLivingDeath);
        bus.addListener(GuardianWingEffects::onPlayerTick);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(CombatContent.GUARDIAN_WING.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.GUARDIAN_WING.get())) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(1.0f);
        player.getCooldowns().addCooldown(CombatContent.GUARDIAN_WING.get(), COOLDOWN_TICKS);

        long now = player.level().getGameTime();
        player.getData(CombatAttachments.GUARDIAN_WING_STASIS).activateUntil(now + STASIS_DURATION_TICKS);
        applyStasis(player, true);
        PacketDistributor.sendToPlayer(player, new StasisSyncPayload(true));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState stasis = player.getData(CombatAttachments.GUARDIAN_WING_STASIS);
        long now = player.level().getGameTime();

        if (stasis.isActive(now)) {
            applyStasis(player, true);
            return;
        }

        if (stasis.hasExpired(now)) {
            stasis.clear();
            applyStasis(player, false);
            player.setHealth(player.getMaxHealth() * REVIVE_HEALTH_PERCENT);
            PacketDistributor.sendToPlayer(player, new StasisSyncPayload(false));
        }
    }

    private static void applyStasis(ServerPlayer player, boolean active) {
        player.setInvulnerable(active);

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        moveSpeed.removeModifier(MOVE_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        moveSpeed.addTransientModifier(new AttributeModifier(
                MOVE_SPEED_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
