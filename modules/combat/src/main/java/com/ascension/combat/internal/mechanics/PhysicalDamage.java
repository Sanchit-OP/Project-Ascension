package com.ascension.combat.internal.mechanics;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Replaces vanilla's own armor-mitigation formula with this module's own curve (docs/technical/combat-api.md
 * section 3) for every hit, universally &mdash; not gated on any weapon, the same way vanilla's own
 * armor mitigation applies to every physical hit regardless of what dealt it. See {@link
 * CombatAttributes#ARMOR}'s javadoc for why this reads a second, uncapped attribute rather than
 * vanilla's own {@code Attributes.ARMOR}.
 *
 * <p>{@link DamageContainer#addModifier} only ever fires when vanilla itself decides an ARMOR
 * reduction applies at all (i.e. the damage source isn't tagged {@code bypasses_armor}), which is
 * exactly the gate this module wants too &mdash; magic and true damage already skip this
 * category entirely on vanilla's own say-so, with no extra check needed here.
 *
 * <p>Registered at {@link EventPriority#HIGHEST} so this modifier is inserted into the {@code
 * ARMOR} reduction's function list before any weapon-specific reduction modifier still registered
 * independently (Mortal Reminder's own arrow-specific ignore, currently the only one left) &mdash;
 * those subtract from or scale whatever reduction already exists, so this module's own base curve
 * has to be computed first for that composition to mean anything. Reads {@code
 * container.getNewDamage()} as the raw damage to mitigate rather than the vanilla-computed {@code
 * reductionIn} it's handed (which this class ignores entirely) &mdash; at this point in the
 * sequence nothing has subtracted from it yet, so it reflects the fully-resolved pre-mitigation
 * amount, bonus procs included.
 *
 * <p><b>Lethality and Penetration, added 2026-09-20.</b> Every attacker-side pen source used to
 * register its own {@code Reduction.ARMOR} modifier (Wraithblade, Edge of Night), each doing its
 * own version of "read the target's Armor, recompute the curve" &mdash; exactly the duplication
 * that let Wraithblade's flat-lethality bug happen in the first place. Now the attacker's {@link
 * CombatAttributes#ARMOR_PENETRATION_PERCENT} and {@link CombatAttributes#LETHALITY} are read once,
 * here, against the target's real Armor stat (percent first, then flat off whatever's left, the
 * same order League itself uses) &mdash; Wraithblade and Edge of Night are two passive contributors
 * to those attributes now, not two separate copies of this math.
 *
 * <p><b>Attack Damage on arrows, added 2026-09-20</b> (Sanchit's own spec: "5 AD means... all the
 * arrows also do 5 more damage"). Melee already gets this for free from vanilla itself &mdash;
 * {@code Player.attack()} reads the live {@code Attributes.ATTACK_DAMAGE} value as the swing's base
 * damage, so any modifier added to it (Sterak's Harness, say) already shows up on a sword without
 * this module doing anything. Vanilla arrows do not: {@code AbstractArrow} damage comes from the
 * bow's own draw-power curve and enchantments, with no reference to the shooter's Attack Damage
 * attribute anywhere in its own code. {@link #applyArrowAttackDamage} closes that gap directly,
 * adding the shooter's current Attack Damage as flat bonus damage to any arrow-based hit, universal
 * and unconditional the same way the rest of this class is &mdash; not gated to this project's own
 * bows, so a vanilla bow or a mob's arrow benefits the same way a stat-model-consistent world
 * implies it should.
 */
public final class PhysicalDamage {

    private PhysicalDamage() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGHEST, PhysicalDamage::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        applyArrowAttackDamage(event);

        LivingEntity target = event.getEntity();
        AttributeInstance armor = target.getAttribute(CombatAttributes.ARMOR);
        if (armor == null) {
            return;
        }

        double effectiveArmor = effectiveArmor(armor.getValue(), event.getSource().getEntity());
        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, reductionIn) ->
                        container.getNewDamage() * (float) (1.0 - DefenseFormula.multiplier(effectiveArmor)));
    }

    private static void applyArrowAttackDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity shooter)) {
            return;
        }

        AttributeInstance attackDamage = shooter.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        event.setAmount(event.getAmount() + (float) attackDamage.getValue());
    }

    private static double effectiveArmor(double armor, Entity attacker) {
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return armor;
        }

        AttributeInstance penetrationPercent = livingAttacker.getAttribute(CombatAttributes.ARMOR_PENETRATION_PERCENT);
        double afterPercent = penetrationPercent == null ? armor : armor * (1.0 - penetrationPercent.getValue());

        AttributeInstance lethality = livingAttacker.getAttribute(CombatAttributes.LETHALITY);
        double afterLethality = lethality == null ? afterPercent : afterPercent - lethality.getValue();

        return Math.max(0.0, afterLethality);
    }
}
