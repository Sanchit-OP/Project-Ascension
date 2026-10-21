package com.ascension.combat.client;

import com.ascension.combat.internal.enchanting.library.LibraryContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client side of the Enchantment Library: its screen. Reached only from the client branch of the mod constructor. */
public final class LibraryClient {

    private LibraryClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(LibraryContent.MENU.get(), LibraryScreen::new));
    }
}
