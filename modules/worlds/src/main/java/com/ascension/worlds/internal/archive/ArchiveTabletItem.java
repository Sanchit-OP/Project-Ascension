package com.ascension.worlds.internal.archive;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A portable copy of one inscription, from loot. Right-click reads it. Which inscription is the
 * {@link ArchiveContent#ARCHIVE_ENTRY} component, set by the loot table that made it
 * ({@code minecraft:set_components}) or by {@code /ascension worlds archive tablet <entry>}.
 *
 * <p>The tooltip names the inscription, so a player can tell two tablets apart in a chest.
 */
public final class ArchiveTabletItem extends Item {

    public ArchiveTabletItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation entry) {
        ItemStack stack = new ItemStack(ArchiveContent.ARCHIVE_TABLET.get());
        stack.set(ArchiveContent.ARCHIVE_ENTRY.get(), entry);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer reader) {
            ArchiveReading.open(reader, stack.get(ArchiveContent.ARCHIVE_ENTRY.get()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component title = ArchiveEntry.find(context.registries(), stack.get(ArchiveContent.ARCHIVE_ENTRY.get()))
                .map(ArchiveEntry::title)
                .orElse(ArchiveEntry.WORN_AWAY);
        tooltip.add(title.copy().withStyle(ChatFormatting.GRAY));
    }
}
