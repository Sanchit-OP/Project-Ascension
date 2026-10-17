package com.ascension.combat.internal.mechanics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tells interested runes that a player was just thrown across the world by an item: {@link Dash}
 * and {@link Blink} both call {@link #moved}. Drop In reads it ("after a dash, blink, ender pearl
 * or a fall"); pearls and falls come from vanilla's own events, but a dash and a blink are this
 * module's, so they announce themselves.
 *
 * <p>Holds callbacks registered once at startup, never a player (ADR-0007 rule 1).
 */
public final class Movements {

    private static final List<Consumer<ServerPlayer>> OBSERVERS = new CopyOnWriteArrayList<>();

    private Movements() {
    }

    public static void addObserver(Consumer<ServerPlayer> observer) {
        OBSERVERS.add(observer);
    }

    /** A dash or blink just moved {@code player}. */
    public static void moved(ServerPlayer player) {
        for (Consumer<ServerPlayer> observer : OBSERVERS) {
            observer.accept(player);
        }
    }
}
