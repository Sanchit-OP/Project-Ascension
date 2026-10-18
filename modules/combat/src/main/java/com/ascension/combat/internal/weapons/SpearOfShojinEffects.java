package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.mechanics.AbilityScaling;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Spear of Shojin's combo counter (docs/technical/combat-api.md section 6): consecutive on-hit
 * strikes build a stack, and the stack resolving pays out as bonus true damage &mdash; damage
 * that bypasses both armor and magic resist (section 2).
 *
 * <p>Listens on {@link LivingDamageEvent.Pre} rather than {@link
 * net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}, unlike Veigo's Blade and
 * Darius's Axe. Those two add a bonus that is the <em>same</em> damage type as the base hit, so
 * folding it into the pre-mitigation amount is correct &mdash; armor should reduce all of it
 * together. A true-damage bonus is different: adding it before mitigation would let vanilla's own
 * armor reduction eat part of a bonus that is supposed to ignore armor entirely. {@code Pre} fires
 * in {@code LivingEntity#actuallyHurt} after armor, enchantment and mob-effect reductions have
 * already been applied to the container but before absorption and before health changes
 * (see NeoForge's own {@code DamageContainer} sequence javadoc) &mdash; the one point where adding
 * more damage lands as true damage rather than getting mitigated a second time.
 *
 * <p>The combo counter itself lives on the attacker, not the target, as {@link
 * StackingChargeState} ({@link CombatAttachments#SHOJIN_STACKS}), and decays if the wielder's next
 * hit doesn't land within the window &mdash; but since the attacker is always a {@link Player},
 * expiry is just checked lazily on the next hit rather than needing Darius's Axe's watch-list:
 * a player is always around to generate the next damage event that would notice the expiry.
 */
public final class SpearOfShojinEffects {

    /**
     * Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec.
     * Every 3rd consecutive hit (was 4th) resolves into a burst of true damage equal to the target's
     * max HP percentage below, then the counter resets. Raised to 5% max HP (was 4%) the same pass,
     * 2026-09-20.
     */
    private static final int STACKS_TO_RESOLVE = 3;

    private static final float TRUE_DAMAGE_MAX_HP_PERCENT = 0.05f;
    private static final long COMBO_WINDOW_TICKS = 60L;

    private SpearOfShojinEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SpearOfShojinEffects::onDamagePre);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.SPEAR_OF_SHOJIN.get())) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = attacker.getData(CombatAttachments.SHOJIN_STACKS);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(STACKS_TO_RESOLVE, now + COMBO_WINDOW_TICKS);

        if (state.stacks() < STACKS_TO_RESOLVE) {
            return;
        }

        state.clear();
        float trueDamage = event.getEntity().getMaxHealth()
                * (TRUE_DAMAGE_MAX_HP_PERCENT + AbilityScaling.SHOJIN_MAX_HP_PER_AD * AbilityScaling.attackDamage(attacker));
        trueDamage = AbilityScaling.capped(trueDamage, attacker);
        event.setNewDamage(event.getNewDamage() + trueDamage);
    }
}
