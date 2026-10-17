package com.ascension.combat.internal.rune.vanguard;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Vanguard's Blooded minor, a <b>growing rune</b> (docs/technical/rune-system.md sections 4.1 and
 * 4.5): omnivamp that grows with every new kind of enemy you defeat, up to a cap. The counting is
 * {@link com.ascension.combat.internal.rune.RuneGrowth}; this class only turns the count into
 * omnivamp.
 *
 * <p>The omnivamp is an attribute modifier, so it is healing through the one shared omnivamp
 * listener like every other source, and it shows on the character sheet under "Runes". Nothing
 * ticks: {@link #refresh} runs when the page is saved, when a kind is counted, and when the player
 * joins or respawns ({@link com.ascension.combat.internal.rune.RuneModifiers}).
 *
 * <p>Its numbers are the rune's data: {@code omnivamp_per_kind}, and {@code growth_cap} on the
 * rune itself.
 */
public final class BloodedEffects {

    public static final ResourceLocation ID = Runes.id("blooded");

    /** Under {@code ascension_combat:rune/}, the prefix the stat sheet reads as "Runes". */
    private static final ResourceLocation MODIFIER_ID = Runes.id("rune/blooded");

    private BloodedEffects() {
    }

    /** Sets the player's Blooded omnivamp to what their page and its growth say: none if it isn't on the page. */
    public static void refresh(ServerPlayer player) {
        double amount = Runes.active(player, ID)
                .map(rune -> player.getData(CombatAttachments.RUNE_PAGE).growth(ID) * rune.value("omnivamp_per_kind", 0.005))
                .orElse(0.0);
        RuneModifiers.set(player, CombatAttributes.OMNIVAMP, MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE);
    }
}
