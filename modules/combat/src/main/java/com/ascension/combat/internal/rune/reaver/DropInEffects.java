package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.mechanics.Movements;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Reaver's Drop In minor (docs/technical/rune-system.md section 4.2): for a few seconds after a
 * dash, a blink, an ender pearl or a fall of several blocks, you cut through armour.
 *
 * <p>The window is Lethality and armour penetration as stat modifiers, so they show on the
 * character sheet under "Runes" and work through the same armour math as an item's. Dashes and
 * blinks announce themselves ({@link Movements}); pearls and falls are vanilla's events. Nothing
 * runs between a trigger and its end but one cheap check on the player's own tick, and that check
 * does not look at the page, so a window ends cleanly even if the rune is swapped out inside it.
 *
 * <p>Its numbers are the rune's data: {@code seconds}, {@code lethality}, {@code penetration}
 * (written as a percent, 10 for 10%), {@code fall_blocks}.
 */
public final class DropInEffects {

    public static final ResourceLocation ID = Runes.id("drop_in");

    private static final ResourceLocation LETHALITY_ID = Runes.id("rune/drop_in_lethality");
    private static final ResourceLocation PENETRATION_ID = Runes.id("rune/drop_in_penetration");

    private DropInEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(DropInEffects::onFall);
        bus.addListener(DropInEffects::onEnderPearl);
        bus.addListener(DropInEffects::onPlayerTick);
        Movements.addObserver(DropInEffects::trigger);
    }

    private static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent() && event.getDistance() >= rune.get().value("fall_blocks", 4.0)) {
            trigger(player);
        }
    }

    private static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        trigger(event.getPlayer());
    }

    /** Opens (or renews) the window, if the player has the rune. */
    private static void trigger(ServerPlayer player) {
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return;
        }

        RuneDefinition values = rune.get();
        player.getData(RuneAttachments.DROP_IN.get())
                .activateUntil(player.level().getGameTime() + values.ticks("seconds", 4.0));
        RuneModifiers.setStat(player, CombatStats.LETHALITY, LETHALITY_ID, values.value("lethality", 10.0));
        RuneModifiers.setStat(player, CombatStats.ARMOR_PENETRATION, PENETRATION_ID, values.value("penetration", 10.0));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DurationState window = player.getData(RuneAttachments.DROP_IN.get());
        if (window.hasExpired(player.level().getGameTime())) {
            window.clear();
            RuneModifiers.setStat(player, CombatStats.LETHALITY, LETHALITY_ID, 0.0);
            RuneModifiers.setStat(player, CombatStats.ARMOR_PENETRATION, PENETRATION_ID, 0.0);
        }
    }
}
