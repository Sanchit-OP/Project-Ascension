package com.ascension.combat.internal.rune.aetherwright;

import com.ascension.combat.internal.rune.Runes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aetherwright's Quickening minor (docs/technical/rune-system.md section 4.3): items' active
 * abilities recharge faster. A {@link com.ascension.combat.internal.mechanics.ItemAbilities.Haste}:
 * every item's active asks for its cooldown through the same door, so all of them benefit.
 *
 * <p>The design asks the stats session for an Item Haste stat (section 11); when it exists this
 * becomes a stat modifier and {@code ItemAbilities} reads the stat instead.
 *
 * <p>Its number is the rune's data: {@code haste}, 0.2 for 20% faster.
 */
public final class QuickeningEffects {

    public static final ResourceLocation ID = Runes.id("quickening");

    private QuickeningEffects() {
    }

    public static double haste(ServerPlayer player) {
        return Runes.active(player, ID).map(rune -> rune.value("haste", 0.20)).orElse(0.0);
    }
}
