package com.ascension.combat.internal.rune;

import com.ascension.combat.api.stat.StatSources;
import com.ascension.combat.internal.rune.vanguard.BloodedEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

    /**
     * Sets {@code entity}'s modifier {@code id} on {@code attribute} to {@code amount}, replacing
     * the one there; zero or less removes it. The one copy of the remove-then-add every rune that
     * gives a stat needs. Transient: it is not saved, so {@link #refresh} puts it back.
     */
    public static void set(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(id);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
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
