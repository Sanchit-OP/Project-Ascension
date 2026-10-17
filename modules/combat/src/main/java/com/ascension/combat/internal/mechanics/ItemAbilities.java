package com.ascension.combat.internal.mechanics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one door every item's <em>active</em> ability goes through when it starts its cooldown
 * (docs/technical/rune-system.md sections 4.3 and 11): the item asks {@link #cooldown} how long its
 * cooldown is, and in asking, tells the world the ability was just used.
 *
 * <p>Two things need that and neither belongs inside an item. Runes that make abilities recharge
 * faster (Quickening) add a {@link Haste}; runes that react to an ability being used (Arc, which
 * the item's active fully charges) add a {@link UseObserver}. The items stay as they were, with
 * one call changed.
 *
 * <p>Holds behaviour only, never a player: the lists are of callbacks registered once at startup
 * (ADR-0007 rule 1). When the stat system grows an Item Haste stat, {@link #cooldown} reads that
 * instead and the rune becomes a stat.
 */
public final class ItemAbilities {

    /** How much faster abilities recharge for a player: 0.2 is 20% faster. */
    @FunctionalInterface
    public interface Haste {
        double fraction(ServerPlayer player);
    }

    /** Told whenever a player uses an item's active ability. */
    @FunctionalInterface
    public interface UseObserver {
        void used(ServerPlayer player);
    }

    private static final List<Haste> HASTES = new CopyOnWriteArrayList<>();
    private static final List<UseObserver> OBSERVERS = new CopyOnWriteArrayList<>();

    private ItemAbilities() {
    }

    public static void addHaste(Haste haste) {
        HASTES.add(haste);
    }

    public static void addUseObserver(UseObserver observer) {
        OBSERVERS.add(observer);
    }

    /**
     * An item's active was just used: tells the observers, and returns the cooldown to start, in
     * ticks, once the player's haste has been counted.
     */
    public static int cooldown(ServerPlayer player, int baseTicks) {
        for (UseObserver observer : OBSERVERS) {
            observer.used(player);
        }

        double haste = 0.0;
        for (Haste source : HASTES) {
            haste += source.fraction(player);
        }
        return scaled(baseTicks, haste);
    }

    /** {@code baseTicks} when abilities recharge {@code haste} faster: 20% faster is a cooldown divided by 1.2. */
    static int scaled(int baseTicks, double haste) {
        return (int) Math.max(1, Math.round(baseTicks / (1.0 + Math.max(0.0, haste))));
    }
}
