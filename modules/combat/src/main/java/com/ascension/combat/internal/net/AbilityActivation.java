package com.ascension.combat.internal.net;

import com.ascension.combat.internal.armor.GargoyleStoneplateEffects;
import com.ascension.combat.internal.armor.RocketbeltEffects;
import com.ascension.combat.internal.armor.ZoniasHourglassEffects;
import com.ascension.combat.internal.weapons.GaleforceEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * What each ability key does on the server: one place's active and nothing else. Each effects class still checks for its own item
 * and its own cooldown, so a key pressed with nothing in that place does nothing. Kept apart from {@link CombatNetwork} so the
 * server checks can press a key without a connection.
 */
public final class AbilityActivation {

    private AbilityActivation() {
    }

    public static void activate(ServerPlayer player, AbilitySlot slot) {
        if (slot == null) {
            return;
        }
        switch (slot) {
            case HELD -> GaleforceEffects.tryActivate(player);
            case CHEST -> GargoyleStoneplateEffects.tryActivate(player);
            case LEGS -> RocketbeltEffects.tryActivate(player);
            case OFF_HAND -> ZoniasHourglassEffects.tryActivate(player);
        }
    }
}
