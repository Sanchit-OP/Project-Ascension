package com.ascension.worlds.internal.archive;

import com.ascension.worlds.internal.net.OpenArchiveEntryPayload;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Opens an inscription on a player's screen. The one place plinths and tablets both go through,
 * so "what reading looks like" has a single entry point.
 */
public final class ArchiveReading {

    /** @param entry the entry to show, or null for an inscription that names nothing */
    public static void open(ServerPlayer player, @Nullable ResourceLocation entry) {
        PacketDistributor.sendToPlayer(player, new OpenArchiveEntryPayload(Optional.ofNullable(entry)));
    }

    private ArchiveReading() {
    }
}
