package com.ascension.worlds.internal.archive;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Operator tools for the Archive, behind {@code /ascension worlds archive}: read lore before the
 * building exists, and build test rooms by hand. Same shape as {@code GatewayDebug}: each returns
 * null on success or the reason it failed, and {@code WorldsCommands} reports it.
 */
public final class ArchiveDebug {

    /** How far the player can be from the plinth they are pointing at. */
    private static final double REACH = 6.0;

    /** Puts a tablet carrying {@code entry} in the player's inventory. */
    @Nullable
    public static Component tablet(ServerPlayer player, ResourceLocation entry) {
        if (ArchiveEntry.find(player.registryAccess(), entry).isEmpty()) {
            return Component.literal("No archive entry " + entry);
        }
        player.getInventory().add(ArchiveTabletItem.of(entry));
        return null;
    }

    /**
     * Points the plinth the player is looking at at {@code entry}. The structure template sets this
     * in real play. An unknown id is allowed on purpose: it is how to see "worn away".
     */
    @Nullable
    public static Component plinth(ServerPlayer player, ResourceLocation entry) {
        if (player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && player.level().getBlockEntity(hit.getBlockPos()) instanceof ArchivePlinthBlockEntity plinth) {
            plinth.setEntry(entry);
            return null;
        }
        return Component.literal("Look at an archive plinth");
    }

    private ArchiveDebug() {
    }
}
