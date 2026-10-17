package com.ascension.combat.internal.rune;

import com.ascension.combat.api.stat.StatSources;
import com.ascension.combat.internal.rune.vanguard.BloodedEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The attribute modifiers runes give, kept right with the page (docs/technical/rune-system.md
 * section 11). A rune that gives a stat adds a modifier whose id sits under {@code
 * ascension_combat:rune/}, which the stat system's breakdown shows as "Runes".
 *
 * <p>Event-driven, never per tick: {@link #refresh} is called when the page is saved, when a
 * growing rune grows, and here when a player respawns (modifiers that are not saved with the
 * player are gone with the old body). Login is refreshed by the login handler, after a legacy
 * page's keystone has been filled in.
 */
public final class RuneModifiers {

    private RuneModifiers() {
    }

    public static void register(IEventBus bus) {
        StatSources.register(Runes.id("rune/"), Component.translatable("stat_source.ascension_combat.runes"));
        bus.addListener(RuneModifiers::onRespawn);
    }

    /** Brings every rune-given modifier on {@code player} in line with their page and growth. */
    public static void refresh(ServerPlayer player) {
        BloodedEffects.refresh(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }
}
