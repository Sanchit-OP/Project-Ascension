package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Aetherwright's Scholar, the path's <b>world keystone</b> (docs/technical/rune-system.md section
 * 4.3): more experience from everything, and enchanting costs a level less. (The lore: the
 * Observers are generous with those who enchant.)
 *
 * <p>Experience is vanilla's own {@link PlayerXpEvent.XpChange}, scaled up. The enchanting table
 * takes its levels through {@link PlayerXpEvent.LevelChange}; while the player has the table open,
 * a charge of levels is made smaller by the discount, but never below one, so enchanting is never
 * free. An anvil has its own menu and is left alone.
 *
 * <p>Its numbers are the rune's data: {@code xp_bonus} (a fraction) and {@code enchant_discount}.
 */
public final class ScholarEffects {

    public static final ResourceLocation ID = Runes.id("scholar");

    private ScholarEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ScholarEffects::onXpChange);
        bus.addListener(ScholarEffects::onLevelChange);
    }

    private static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            event.setAmount(boosted(event.getAmount(), rune.get().value("xp_bonus", 0.5)));
        }
    }

    private static void onLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.getLevels() >= 0 || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof EnchantmentMenu)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            event.setLevels(discounted(event.getLevels(), rune.get().count("enchant_discount", 1)));
        }
    }

    /** {@code amount} of experience with {@code bonus} more, rounded to the nearest point. */
    static int boosted(int amount, double bonus) {
        return (int) Math.round(amount * (1.0 + bonus));
    }

    /**
     * A charge of {@code levels} (negative) made {@code discount} smaller, never below a charge of
     * one: -3 becomes -2, -1 stays -1.
     */
    static int discounted(int levels, int discount) {
        return Math.min(-1, levels + discount);
    }
}
