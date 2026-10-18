package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.hud.RuneHudEntry;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a player's equipped runes are doing, sent to that player only, and only when it changes
 * (ADR-0007: delta-synced, never per tick). The whole list goes each time: it is a handful of
 * small entries, and a list that always replaces the last is one that cannot drift.
 *
 * @param entries one per equipped rune that has something to show, keystone first
 */
public record RuneHudSyncPayload(List<RuneHudEntry> entries) implements CustomPacketPayload {

    /** More than a page can hold, so a bad packet cannot ask the client to allocate without limit. */
    public static final int MAX_ENTRIES = 12;

    public static final CustomPacketPayload.Type<RuneHudSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune_hud_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RuneHudSyncPayload> STREAM_CODEC = StreamCodec.composite(
            RuneHudEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), RuneHudSyncPayload::entries,
            RuneHudSyncPayload::new);

    public RuneHudSyncPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
