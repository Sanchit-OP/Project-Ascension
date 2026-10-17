package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Aetherwright's Clarity minor (docs/technical/rune-system.md section 4.3): while you are above a
 * share of your health, more Attack Damage and more magic damage.
 *
 * <p>The Attack Damage is a stat modifier that comes and goes with your health, so it is checked
 * on your own tick, but <em>only changed when the answer changes</em> (an attribute that is
 * rewritten every tick would be re-synced every tick). The magic half is a {@link
 * com.ascension.combat.internal.mechanics.MagicDamage.Booster} that asks the same question when the
 * damage is dealt.
 *
 * <p>Its numbers are the rune's data: {@code above_health} (a fraction), {@code attack_damage},
 * {@code magic_bonus}.
 */
public final class ClarityEffects {

    public static final ResourceLocation ID = Runes.id("clarity");

    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/clarity_attack_damage");

    private ClarityEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ClarityEffects::onPlayerTick);
    }

    public static float boost(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Player player)) {
            return 1.0f;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        return rune.isPresent() && isClear(player, rune.get()) ? 1.0f + (float) rune.get().value("magic_bonus", 0.08) : 1.0f;
    }

    private static boolean isClear(LivingEntity player, RuneDefinition values) {
        return above(player.getHealth(), StatReader.value(player, CombatStats.HEALTH), values.value("above_health", 0.7));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AttributeInstance attackDamage = player.getAttribute(CombatStats.ATTACK_DAMAGE.attribute());
        if (attackDamage == null) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        boolean want = rune.isPresent() && isClear(player, rune.get());
        if (want != attackDamage.hasModifier(MODIFIER_ID)) {
            RuneModifiers.setStat(player, CombatStats.ATTACK_DAMAGE, MODIFIER_ID,
                    want ? rune.get().value("attack_damage", 1.0) : 0.0);
        }
    }

    /** Whether {@code health} of {@code max} is above the {@code threshold} fraction. */
    static boolean above(double health, double max, double threshold) {
        return max > 0.0 && health / max > threshold;
    }
}
