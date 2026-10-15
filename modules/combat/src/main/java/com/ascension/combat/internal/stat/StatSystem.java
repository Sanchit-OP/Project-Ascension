package com.ascension.combat.internal.stat;

import net.neoforged.bus.api.IEventBus;

/**
 * Wires up item stats (docs/technical/item-stats.md, ADR-0019): the data maps, the listener that
 * gives every item its stats, and the two fixes for vanilla timing (gear after {@code /reload},
 * health after login). The one call {@code AscensionCombat} makes for all of it.
 */
public final class StatSystem {

    private StatSystem() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        StatDataMaps.register(modBus);
        HealthAcrossLogin.register(modBus, gameBus);
        ItemStatModifiers.register(gameBus);
        WornGear.register(gameBus);
    }
}
