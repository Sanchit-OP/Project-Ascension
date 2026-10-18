package com.ascension.combat.client.dev;

import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * Keeps other mods' development-only greetings out of the dev client's chat. FTB Library says
 * "Hello from FTB Library!" to every player who joins, but only when it runs in a development
 * environment ({@code Platform.isDevelopmentEnvironment()}, with no setting to turn it off), so a
 * player of the finished pack never sees it; this is the same condition, so it is dormant there too.
 */
public final class DevChatFilter {

    /** The exact text of each greeting, as the server sends it. */
    private static final Set<String> DEV_ONLY_GREETINGS = Set.of("Hello from FTB Library!");

    private DevChatFilter() {
    }

    public static void register(IEventBus gameBus) {
        if (FMLLoader.isProduction()) {
            return;
        }

        gameBus.addListener(DevChatFilter::onSystemChat);
    }

    private static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (DEV_ONLY_GREETINGS.contains(event.getMessage().getString())) {
            event.setCanceled(true);
        }
    }
}
