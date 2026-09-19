package com.ascension.combat.internal.weapons;

import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.MagicDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Test Magic Blade's identity: deals a flat 10 magic damage and nothing else &mdash; not a
 * catalog item (see {@link CombatContent#TEST_MAGIC_BLADE}'s javadoc), added 2026-09-19 purely to
 * watch {@link com.ascension.combat.internal.mechanics.DefenseFormula} and Magic Resist reduce a
 * known, isolated number without a physical component in the same hit.
 *
 * <p>Zeroes out whatever physical damage this hit would otherwise have carried before adding the
 * flat magic amount &mdash; {@link LivingDamageEvent.Pre} fires post-armor, so at this point the
 * weapon's own (near-zero, see the item's own attributes) base damage has already been through
 * {@link com.ascension.combat.internal.mechanics.PhysicalDamage}'s reduction; setting it to zero
 * here just guarantees the number on screen is only ever the magic component this class adds.
 */
public final class TestMagicBladeEffects {

    /** Deliberately not a "Placeholder" comment like the catalog's own items -- this number is
     * only ever meant to be read raw, to verify the reduction math, not tuned by play. */
    private static final float FLAT_MAGIC_DAMAGE = 10.0f;

    private TestMagicBladeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(TestMagicBladeEffects::onDamagePre);
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.TEST_MAGIC_BLADE.get())) {
            return;
        }

        event.setNewDamage(0.0f);
        MagicDamage.apply(event, FLAT_MAGIC_DAMAGE);
    }
}
