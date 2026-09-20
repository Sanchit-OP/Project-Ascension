package com.ascension.combat.client;

import com.ascension.combat.internal.net.AbilityKey;
import java.util.EnumMap;
import java.util.Map;

/**
 * The client's own mirror of each bow-active cooldown (docs/technical/combat-api.md section 6),
 * kept current by {@link com.ascension.combat.internal.net.AbilityCooldownSyncPayload} and read by
 * {@link AbilityCooldownDecorator} &mdash; the client-side half of not routing these cooldowns
 * through vanilla {@code ItemCooldowns}.
 */
public final class ClientAbilityCooldowns {

    private record Cooldown(long endsAtGameTime, int durationTicks) {
    }

    private static final Map<AbilityKey, Cooldown> COOLDOWNS = new EnumMap<>(AbilityKey.class);

    private ClientAbilityCooldowns() {
    }

    public static void update(AbilityKey key, long endsAtGameTime, int durationTicks) {
        COOLDOWNS.put(key, new Cooldown(endsAtGameTime, durationTicks));
    }

    /** The fraction of the cooldown still remaining, {@code 0} once it has fully lapsed. */
    public static float fractionRemaining(AbilityKey key, long now) {
        Cooldown cooldown = COOLDOWNS.get(key);
        if (cooldown == null || cooldown.durationTicks() <= 0) {
            return 0.0f;
        }

        long remaining = cooldown.endsAtGameTime() - now;
        if (remaining <= 0) {
            return 0.0f;
        }

        return Math.min(1.0f, (float) remaining / cooldown.durationTicks());
    }
}
