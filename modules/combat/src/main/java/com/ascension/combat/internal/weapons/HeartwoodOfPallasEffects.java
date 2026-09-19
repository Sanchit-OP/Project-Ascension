package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Heartwood of Pallas's identity (docs/technical/combat-api.md section 6): on-hit small magic
 * damage plus an uncapped stack of corruption on the target, refreshed within a 5s window; a shot
 * drawn for 3x the normal time consumes every corruption stack on its target as magic damage equal
 * to that target's missing HP.
 *
 * <p>Corruption reuses {@link StackingChargeState} exactly as Darius's Axe does, just with {@link
 * Integer#MAX_VALUE} as the cap instead of a real one &mdash; same "count and expiry" shape, no
 * new class needed. Unlike the axe's shred, corruption is never a live attribute effect, so
 * there's nothing that needs a per-tick watcher to turn off on expiry: staleness is checked lazily,
 * the same way {@link SpearOfShojinEffects} checks its own combo window.
 *
 * <p><b>Reading the charge.</b> {@link ArrowLooseEvent} fires inside vanilla's own {@code
 * BowItem#releaseUsing} with the exact tick count the bow was held, before the arrow exists.
 * {@link #onArrowLoose} reads that directly rather than re-deriving it from {@code
 * getPowerForTime} (which saturates at {@link BowItem#MAX_DRAW_DURATION} and can't tell a 20-tick
 * draw from a 60-tick one). Because the event fires before the arrow is spawned, "this shot is
 * charged" has to travel through two hops: {@link CombatAttachments#HEARTWOOD_CHARGE_PENDING} on
 * the wielder bridges from the event to the arrow's {@link EntityJoinLevelEvent} moments later in
 * the same call stack, and {@link CombatAttachments#HEARTWOOD_CHARGED_ARROW} on the arrow itself
 * carries it the rest of the way to the eventual {@link LivingDamageEvent.Pre}, since the arrow
 * may still be in flight after the wielder has already drawn and released another, uncharged,
 * shot.
 *
 * <p><b>Feedback, added after Sanchit's first test pass 2026-09-12.</b> Two real problems: the
 * charge threshold felt unusably slow in an actual fight ("till when i do a 3x pull it will be
 * gg's"), and there was no way to perceive corruption building up at all. Fixed with three
 * things, none of which touch the underlying mechanic: {@link #CHARGE_THRESHOLD_TICKS} dropped
 * from a literal 3x{@code MAX_DRAW_DURATION} (60 ticks) to a much shorter placeholder; {@link
 * #onPlayerTick} plays a one-shot personal "ready" chime the exact tick a draw crosses the
 * threshold, so releasing on time doesn't require watching a timer; and {@link #onDamagePre} now
 * sends the shooter an action-bar readout of the target's current stack count and spawns a small
 * particle burst on the target itself, visible to anyone nearby.
 */
public final class HeartwoodOfPallasEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float ON_HIT_MAGIC_DAMAGE = 3.0f;

    /** Catalog-specified: refreshed while the target's been hit in the last 5s. */
    private static final long CORRUPTION_DECAY_TICKS = 100L;

    /**
     * Placeholder, lowered from a literal 3x {@link BowItem#MAX_DRAW_DURATION} (60 ticks) after
     * Sanchit's first test pass found that draw unusably slow in a real fight.
     */
    private static final int CHARGE_THRESHOLD_TICKS = 25;

    private HeartwoodOfPallasEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(HeartwoodOfPallasEffects::onArrowLoose);
        bus.addListener(HeartwoodOfPallasEffects::onEntityJoinLevel);
        bus.addListener(HeartwoodOfPallasEffects::onDamagePre);
        bus.addListener(HeartwoodOfPallasEffects::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack usingItem = player.getUseItem();
        if (!player.isUsingItem() || !usingItem.is(CombatContent.HEARTWOOD_OF_PALLAS.get())) {
            return;
        }

        int ticksUsing = usingItem.getUseDuration(player) - player.getUseItemRemainingTicks();
        if (ticksUsing == CHARGE_THRESHOLD_TICKS) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    private static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getCharge() < CHARGE_THRESHOLD_TICKS) {
            return;
        }

        if (!event.getBow().is(CombatContent.HEARTWOOD_OF_PALLAS.get())) {
            return;
        }

        event.getEntity().getData(CombatAttachments.HEARTWOOD_CHARGE_PENDING).arm();
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.getOwner() instanceof Player shooter)) {
            return;
        }

        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !weapon.is(CombatContent.HEARTWOOD_OF_PALLAS.get())) {
            return;
        }

        if (shooter.getData(CombatAttachments.HEARTWOOD_CHARGE_PENDING).consume()) {
            arrow.getData(CombatAttachments.HEARTWOOD_CHARGED_ARROW).arm();
        }
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || !weapon.is(CombatContent.HEARTWOOD_OF_PALLAS.get())) {
            return;
        }

        MagicDamage.apply(event, ON_HIT_MAGIC_DAMAGE);

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState corruption = target.getData(CombatAttachments.HEARTWOOD_CORRUPTION);
        long now = serverLevel.getGameTime();
        if (corruption.expiresAtGameTime() <= now) {
            corruption.clear();
        }
        corruption.refresh(Integer.MAX_VALUE, now + CORRUPTION_DECAY_TICKS);

        serverLevel.sendParticles(
                ParticleTypes.SOUL, target.getX(), target.getY(1.0), target.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
        if (source.getEntity() instanceof ServerPlayer shooter) {
            shooter.displayClientMessage(
                    Component.literal("Corruption: " + corruption.stacks()), true);
        }

        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        if (!arrow.getData(CombatAttachments.HEARTWOOD_CHARGED_ARROW).isArmed()) {
            return;
        }

        if (corruption.stacks() <= 0) {
            return;
        }

        corruption.clear();
        float missingHealth = target.getMaxHealth() - target.getHealth();
        if (missingHealth > 0.0f) {
            MagicDamage.apply(event, missingHealth);
        }
    }
}
