package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
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
 *
 * <p>Its numbers are the rune's data: {@code attack_speed} (a fraction, 0.2 = +20%) and
 * {@code seconds}. The burst ends early if the rune leaves the page.
 */
public final class KillingSurgeEffects {

    public static final ResourceLocation ID = Runes.id("killing_surge");

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune/killing_surge_attack_speed");

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

        Optional<RuneDefinition> rune = Runes.active(killer, ID);
        if (rune.isEmpty()) {
            return;
        }

        killer.getData(CombatAttachments.KILLING_SURGE_BURST)
                .activateUntil(killer.level().getGameTime() + rune.get().ticks("seconds", 3.0));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        double bonus = 0.0;
        if (player.getData(CombatAttachments.KILLING_SURGE_BURST).isActive(player.level().getGameTime())) {
            bonus = Runes.active(player, ID).map(rune -> rune.value("attack_speed", 0.2)).orElse(0.0);
        }
        applyModifier(player, bonus);
    }

    private static void applyModifier(ServerPlayer player, double bonus) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
        if (bonus <= 0.0) {
            return;
        }

        attackSpeed.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
