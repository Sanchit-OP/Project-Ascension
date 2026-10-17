package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * Reaver's Pounce minor (docs/technical/rune-system.md section 4.2): your critical hits, the
 * falling ones, hit harder. Vanilla's critical multiplier is 1.5; Pounce raises it.
 *
 * <p>It only ever raises the multiplier (a bigger one from another source stays), and it reads
 * after nothing else: it is a plain listener on the event that decides how hard a critical hit is.
 *
 * <p>Its number is the rune's data: {@code crit_multiplier}.
 */
public final class PounceEffects {

    public static final ResourceLocation ID = Runes.id("pounce");

    private PounceEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(PounceEffects::onCriticalHit);
    }

    private static void onCriticalHit(CriticalHitEvent event) {
        if (!event.isCriticalHit() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            event.setDamageMultiplier(Math.max(event.getDamageMultiplier(), (float) rune.get().value("crit_multiplier", 1.75)));
        }
    }
}
