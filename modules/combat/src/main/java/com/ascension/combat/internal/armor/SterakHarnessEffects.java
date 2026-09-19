package com.ascension.combat.internal.armor;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.DurationState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sterak's Harness's identity (docs/technical/combat-api.md section 6): converts a percentage of
 * max HP into bonus attack damage while worn; taking a big hit auto-triggers a shield (vanilla
 * absorption, same as every other Shield/Absorb consumer) plus a temporary lifesteal window, on a
 * cooldown.
 *
 * <p>The last item in the catalog, and the last to not need any genuinely new mechanic: the AD
 * conversion is the same "flat modifier recomputed every tick while worn" idiom used throughout
 * this catalog, just derived from the wearer's own max HP instead of a fixed constant, so it can't
 * reuse {@link StatBoots} directly. The shield reuses vanilla absorption. The lifesteal window
 * reuses {@link DurationState}.
 *
 * <p><b>The lifesteal is implemented as omnivamp-shaped</b> (heals off any damage dealt while the
 * window is active, the same as {@link com.ascension.combat.internal.weapons.EndlessHungerEffects}
 * and {@link TastedBloodEffects}), not physical-only the way "lifesteal" is normally distinguished
 * from "omnivamp" in combat-api.md section 3. Nothing in this catalog tags a damage instance's
 * type generically yet &mdash; each weapon just knows whether its own proc is physical or magic at
 * the point it adds it &mdash; so telling "was this specific hit physical" apart at an arbitrary
 * later point (here, in a generic {@link LivingDamageEvent.Post} listener with no idea which item
 * dealt the damage) isn't a small addition. Flagged rather than built around for one ten-second
 * buff on the catalog's last item.
 */
public final class SterakHarnessEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final float MAX_HEALTH_TO_ATTACK_DAMAGE_PERCENT = 0.10f;

    private static final float BIG_HIT_THRESHOLD_PERCENT = 0.15f;
    private static final float SHIELD_MAX_HEALTH_PERCENT = 0.15f;

    /** Catalog-specified: 10s. */
    private static final long LIFESTEAL_DURATION_TICKS = 200L;

    /** Catalog-specified: 10%. */
    private static final float LIFESTEAL_PERCENT = 0.10f;

    private static final int COOLDOWN_TICKS = 1200;

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(AscensionCombat.MOD_ID, "steraks_harness_attack_damage");

    private SterakHarnessEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(SterakHarnessEffects::onPlayerTick);
        bus.addListener(SterakHarnessEffects::onDamagePost);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        if (!player.getItemBySlot(EquipmentSlot.OFFHAND).is(CombatContent.STERAKS_HARNESS.get())) {
            return;
        }

        attackDamage.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                player.getMaxHealth() * MAX_HEALTH_TO_ATTACK_DAMAGE_PERCENT,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0f) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target
                && target.getItemBySlot(EquipmentSlot.OFFHAND).is(CombatContent.STERAKS_HARNESS.get())) {
            maybeTriggerShield(target, event.getNewDamage());
        }

        if (event.getSource().getEntity() instanceof Player attacker
                && attacker.getItemBySlot(EquipmentSlot.OFFHAND).is(CombatContent.STERAKS_HARNESS.get())
                && attacker.getData(CombatAttachments.STERAKS_LIFESTEAL).isActive(attacker.level().getGameTime())) {
            attacker.heal(event.getNewDamage() * LIFESTEAL_PERCENT);
        }
    }

    private static void maybeTriggerShield(ServerPlayer player, float damageTaken) {
        if (damageTaken < player.getMaxHealth() * BIG_HIT_THRESHOLD_PERCENT) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(CombatContent.STERAKS_HARNESS.get())) {
            return;
        }

        float shield = player.getMaxHealth() * SHIELD_MAX_HEALTH_PERCENT;
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), shield));
        player.getData(CombatAttachments.STERAKS_LIFESTEAL)
                .activateUntil(player.level().getGameTime() + LIFESTEAL_DURATION_TICKS);
        player.getCooldowns().addCooldown(CombatContent.STERAKS_HARNESS.get(), COOLDOWN_TICKS);
    }
}
