package com.ascension.atmosphere.api;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Finds the oxygen sources available to a player.
 *
 * <p>Register one to contribute sources from anywhere &mdash; an inventory slot, a curio, a
 * suit module, a nearby vehicle &mdash; without this module knowing your storage exists.
 *
 * <p>Called on the server thread during oxygen accounting. Keep it cheap and allocate as
 * little as possible; collectors run for every player with a supply.
 */
@FunctionalInterface
public interface OxygenSourceCollector {

    /**
     * Pass every source this collector can find to {@code sink}.
     *
     * <p>Sources handed to the sink are used immediately and not retained.
     */
    void collect(ServerPlayer player, Consumer<OxygenSource> sink);
}
