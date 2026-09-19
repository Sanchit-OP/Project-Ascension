package com.ascension.combat.internal.rune;

import com.ascension.combat.api.rune.RunePath;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One entry in {@link RuneCatalog}: a rune's identity (id, path, slot, display name) with no
 * mechanic attached &mdash; the mechanic lives in that rune's own {@code XyzEffects} class under
 * {@code internal.rune.<path>}, same split every weapon and armor item in this module already
 * uses between its {@code CombatContent} registration and its effects class.
 */
public record RuneDefinition(ResourceLocation id, RunePath path, RuneSlot slot, Component displayName) {
}
