package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.mechanics.Shield;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Bulwark's Overheal keystone, the third on the dial (replacing Cat's Eye on 2026-10-22; Sanchit: creepers and phantoms keeping away
 * "was a gimmick, very useless"; the idea is his): healing becomes a shield.
 *
 * <ul>
 *   <li><b>Healing that would go past full health becomes a shield</b> instead of being wasted (omnivamp at full health, the
 *       Divine Sunderer's heal, Second Wind's last point).</li>
 *   <li><b>{@code convert_share} of all other healing is added as a shield as well</b>, so it works in a boss fight where you are
 *       never at full health.</li>
 * </ul>
 *
 * <p>The shield is vanilla's absorption (the golden hearts), through {@link Shield}, and it holds up to {@code shield_cap} of your
 * maximum health (never above {@code cap_absolute}). It does not fade: it is spent by damage, and refilled by healing. Read late
 * ({@link EventPriority#LOW}) so Revitalize's stronger heal is what converts. Nothing is kept between heals (ADR-0007).
 */
public final class OverhealEffects {
    public static final ResourceLocation ID = Runes.id("overheal");

    private OverhealEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, OverhealEffects::onHeal);
    }

    private static void onHeal(LivingHealEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }
        RuneDefinition values = rune.get();
        float max = player.getMaxHealth();
        float gain = shieldFrom(event.getAmount(), max - player.getHealth(), values.value("convert_share", 0.3));
        float cap = cap(max, values.value("shield_cap", 0.4), values.value("cap_absolute", 100.0));
        if (gain > 0.0f) {
            Shield.add(player, gain, cap);
        }
    }

    /**
     * The shield a heal gives: everything past full health, and {@code convertShare} of what filled the missing health. A heal of 30
     * with 10 missing is 20 over plus a share of 10.
     */
    static float shieldFrom(float heal, float missing, double convertShare) {
        float room = Math.max(0.0f, missing);
        float overflow = Math.max(0.0f, heal - room);
        float used = heal - overflow;
        return overflow + used * (float) convertShare;
    }

    /** The most shield the rune holds: a share of maximum health, never above {@code ceiling}. */
    static float cap(float maxHealth, double share, double ceiling) {
        return (float) Math.min(ceiling, maxHealth * share);
    }
}
