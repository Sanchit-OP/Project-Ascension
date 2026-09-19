package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Vanguard's Killing Surge minor (docs/technical/combat-api.md section 7): a kill grants a short
 * attack-speed burst. Same burst-window shape as Edge of Night's post-absorb burst ({@link
 * DurationState} set on the triggering event, an {@link AttributeModifier} applied and removed
 * every player tick based on whether the window is still active).
 */
public final class KillingSurgeEffects {

    /** Placeholder -- combat-api.md section 3: Sanchit tunes by playing, not by spec. */
    private static final long BURST_DURATION_TICKS = 60L;

    private static final double ATTACK_SPEED_BONUS_PERCENT = 0.2;

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "killing_surge_attack_speed");

    private KillingSurgeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(KillingSurgeEffects::onLivingDeath);
        bus.addListener(KillingSurgeEffects::onPlayerTick);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        if (!killer.getData(CombatAttachments.RUNE_PAGE).hasMinor(RuneCatalog.VANGUARD_KILLING_SURGE.id())) {
            return;
        }

        killer.getData(CombatAttachments.KILLING_SURGE_BURST)
                .activateUntil(killer.level().getGameTime() + BURST_DURATION_TICKS);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean active = player.getData(CombatAttachments.KILLING_SURGE_BURST).isActive(player.level().getGameTime());
        applyModifier(player, active);
    }

    private static void applyModifier(ServerPlayer player, boolean active) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        if (!active) {
            return;
        }

        attackSpeed.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID, ATTACK_SPEED_BONUS_PERCENT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
