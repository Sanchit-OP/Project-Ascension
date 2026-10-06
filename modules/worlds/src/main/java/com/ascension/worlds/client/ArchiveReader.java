package com.ascension.worlds.client;

import com.ascension.worlds.internal.archive.ArchiveEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Shows an archive entry in vanilla's book screen: the title, bold, heads the first page. Reusing
 * the book keeps reading familiar (page turns, keyboard, narration) and costs no art; a
 * tablet-shaped reading screen is a later design job if the book feels wrong.
 *
 * <p>The entry is looked up in the client's own synced copy of the {@code archive_entry}
 * registry. An id it does not know reads as worn away, never as an error.
 */
final class ArchiveReader {

    static void open(@Nullable ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ArchiveEntry entry = ArchiveEntry.find(minecraft.level.registryAccess(), id).orElse(null);
        minecraft.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages(entry))));
    }

    private static List<Component> pages(@Nullable ArchiveEntry entry) {
        if (entry == null) {
            return List.of(ArchiveEntry.WORN_AWAY.copy().withStyle(ChatFormatting.ITALIC));
        }
        List<Component> pages = new ArrayList<>(entry.pages().size());
        for (int i = 0; i < entry.pages().size(); i++) {
            Component page = entry.pages().get(i);
            pages.add(i == 0
                    ? Component.empty()
                            .append(entry.title().copy().withStyle(ChatFormatting.BOLD))
                            .append("\n\n")
                            .append(page)
                    : page);
        }
        return pages;
    }

    private ArchiveReader() {
    }
}
