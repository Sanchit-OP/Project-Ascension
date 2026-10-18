package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.ItemAbilities;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.ArmedState;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.net.AbilityCooldownSyncPayload;
import com.ascension.combat.internal.net.AbilityKey;
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
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Heartwood of Pallas's identity (docs/technical/combat-api.md section 6): on-hit small magic
 * damage plus an uncapped stack of corruption on the target, refreshed within a 5s window; a
 * charged shot consumes every corruption stack on its target for magic damage equal to that
 * stack count as a percentage of the target's missing HP (10 stacks -&gt; 10% of missing HP, 100
 * stacks -&gt; the full missing HP, uncapped past that) -- see {@link #STACK_PERCENT_DIVISOR}.
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
 *
 * <p><b>Second pass, after Sanchit's next test 2026-09-20.</b> Three changes:
 * <ul>
 *   <li>{@link #CHARGE_THRESHOLD_TICKS} raised 25 -&gt; 35: the shorter draw from the first pass
 *       turned out too easy to hit by accident.</li>
 *   <li>The charged shot is now a real ability with a cooldown ({@link
 *       #CHARGE_COOLDOWN_TICKS}, 15s), armed the instant a charged shot is loosed rather than on
 *       hit -- a cast-time cooldown, not a hit-confirm one, the same way a League ability goes on
 *       cooldown whether or not it connects. While it's active, holding the draw past threshold
 *       produces an ordinary arrow: no ready chime ({@link #onPlayerTick}), no charged-arrow flag
 *       ({@link #onArrowLoose}) -- the draw itself is never blocked, only what it produces.</li>
 *   <li>Fixed a real bug in {@link #onDamagePre}, found by Sanchit stacking corruption and then
 *       firing repeated charged shots: the corruption stack was being refreshed (+1) for
 *       <em>every</em> hit, charged or not, before the execute check ran -- so a charged shot
 *       against a target with zero prior corruption would still see {@code stacks() == 1} (its
 *       own contribution) and execute anyway, and every subsequent charged shot did the same
 *       regardless of real build-up. The execute check now reads the stack count from <em>before</em>
 *       this hit's own contribution, and only refreshes the stack when the hit does not execute.
 * </ul>
 *
 * <p><b>Third pass, same day.</b> A flat "missing HP, full stop" execute made the stack count
 * itself pointless -- one stack was exactly as strong as a hundred. The payoff is now scaled by
 * how many stacks are actually being consumed ({@link #STACK_PERCENT_DIVISOR}), so a lightly
 * stacked target only takes a sliver of their missing HP and a heavily corrupted one takes most
 * or all of it -- which is also why corruption being uncapped (the whole point of using {@link
 * Integer#MAX_VALUE} as its cap) now actually matters: past 100 stacks the execute deals more
 * than the target's current missing HP, deliberately.
 *
 * <p><b>Fourth pass, 2026-09-20.</b> First tried switching the cooldown to vanilla {@code
 * ItemCooldowns} purely to get the same hotbar swipe every sword already has. That broke ordinary
 * shooting outright: {@code ItemCooldowns} gates the whole item's right-click use, not just this
 * ability, and a bow's <em>normal</em> draw goes through that exact same interaction path (unlike a
 * sword, whose normal attack is left-click and never touches it) &mdash; confirmed in vanilla's own
 * {@code ServerPlayerGameMode}, which skips {@code Item#use} outright while on cooldown. Reverted
 * to a private cooldown ({@link CombatAttachments#HEARTWOOD_CHARGE_COOLDOWN}) so the bow stays
 * fully usable, and paired it with {@link com.ascension.combat.client.AbilityCooldownDecorator},
 * which draws the identical swipe by hand from {@link AbilityCooldownSyncPayload} instead.
 */
public final class HeartwoodOfPallasEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float ON_HIT_MAGIC_DAMAGE = 3.0f;

    /** Catalog-specified: refreshed while the target's been hit in the last 5s. */
    private static final long CORRUPTION_DECAY_TICKS = 100L;

    /**
     * Placeholder, lowered from a literal 3x {@link BowItem#MAX_DRAW_DURATION} (60 ticks) after
     * Sanchit's first test pass found that draw unusably slow in a real fight, then raised again
     * (25 -&gt; 35) after the second pass found the shorter draw too easy to trigger by accident.
     */
    private static final int CHARGE_THRESHOLD_TICKS = 35;

    /** Placeholder -- how long after loosing a charged shot before another can be armed. */
    private static final int CHARGE_COOLDOWN_TICKS = 300;

    /**
     * Catalog-specified shape (not itself a placeholder, though the numbers it divides are): the
     * execute deals {@code stacks / STACK_PERCENT_DIVISOR} of the target's missing HP, so this is
     * "how many stacks equal a 100% missing-HP execute" -- 100 means 10 stacks is a 10% execute.
     */
    private static final float STACK_PERCENT_DIVISOR = 100.0f / 1.5f;   // 1.5% of the missing health per stack

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
        if (ticksUsing != CHARGE_THRESHOLD_TICKS) {
            return;
        }

        DurationState cooldown = player.getData(CombatAttachments.HEARTWOOD_CHARGE_COOLDOWN);
        if (cooldown.isActive(player.level().getGameTime())) {
            return;
        }

        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    private static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!event.getBow().is(CombatContent.HEARTWOOD_OF_PALLAS.get())) {
            return;
        }

        if (event.getCharge() < CHARGE_THRESHOLD_TICKS) {
            return;
        }

        long now = player.level().getGameTime();
        DurationState cooldown = player.getData(CombatAttachments.HEARTWOOD_CHARGE_COOLDOWN);
        if (cooldown.isActive(now)) {
            return;
        }

        int cooldownTicks = ItemAbilities.cooldown(player, CHARGE_COOLDOWN_TICKS);
        cooldown.activateUntil(now + cooldownTicks);
        PacketDistributor.sendToPlayer(
                player, new AbilityCooldownSyncPayload(AbilityKey.HEARTWOOD, now + cooldownTicks, cooldownTicks));
        player.getData(CombatAttachments.HEARTWOOD_CHARGE_PENDING).arm();
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

        MagicDamage.apply(event,
                ON_HIT_MAGIC_DAMAGE + AbilityScaling.HEARTWOOD_ON_HIT_PER_AD * AbilityScaling.attackDamage(source.getEntity()));

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState corruption = target.getData(CombatAttachments.HEARTWOOD_CORRUPTION);
        long now = serverLevel.getGameTime();
        if (corruption.expiresAtGameTime() <= now) {
            corruption.clear();
        }

        // Read before this hit adds its own contribution -- a charged shot only executes stacks
        // that already existed from earlier hits, never the stack this very hit would produce.
        boolean isCharged = source.getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getData(CombatAttachments.HEARTWOOD_CHARGED_ARROW).isArmed();
        boolean executes = isCharged && corruption.stacks() > 0;

        if (executes) {
            int stacksConsumed = corruption.stacks();
            corruption.clear();
            float missingHealth = target.getMaxHealth() - target.getHealth();
            float executeDamage = Math.min(missingHealth * (stacksConsumed / STACK_PERCENT_DIVISOR),
                    AbilityScaling.HUNT_CAP_PER_AD * AbilityScaling.attackDamage(source.getEntity()));
            if (executeDamage > 0.0f) {
                MagicDamage.apply(event, executeDamage);
            }
        } else {
            corruption.refresh(Integer.MAX_VALUE, now + CORRUPTION_DECAY_TICKS);
        }

        serverLevel.sendParticles(
                ParticleTypes.SOUL, target.getX(), target.getY(1.0), target.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
        if (source.getEntity() instanceof ServerPlayer shooter) {
            shooter.displayClientMessage(
                    Component.literal("Corruption: " + corruption.stacks()), true);
        }
    }
}
