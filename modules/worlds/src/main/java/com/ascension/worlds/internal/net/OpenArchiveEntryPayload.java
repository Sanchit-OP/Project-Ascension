package com.ascension.worlds.internal.net;

import com.ascension.worlds.AscensionWorlds;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server &rarr; client: show this archive entry. Empty means the inscription is unreadable (a
 * plinth or tablet that names nothing). The client resolves the id against its own synced copy of
 * the {@code archive_entry} registry, so the text itself never travels here, and an id the client
 * does not know reads as worn away.
 */
public record OpenArchiveEntryPayload(Optional<ResourceLocation> entry) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenArchiveEntryPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "open_archive_entry"));

    public static final StreamCodec<ByteBuf, OpenArchiveEntryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), OpenArchiveEntryPayload::entry,
            OpenArchiveEntryPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
