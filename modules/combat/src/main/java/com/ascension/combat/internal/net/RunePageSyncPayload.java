package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.rune.RunePageState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells a player's own client what their currently saved rune page is (docs/technical/combat-api.md
 * section 7), sent once on login so {@link com.ascension.combat.client.RuneScreen} can pre-fill
 * itself with the player's real saved selection rather than opening blank every time &mdash;
 * mirrors why {@link StasisSyncPayload} exists: server-authoritative state the client has no other
 * way to know.
 *
 * <p>Same shape as {@link SetRunePagePayload} except {@code primaryPath} is nullable too (encoded
 * the same {@code -1}-sentinel way as its secondary path) &mdash; a brand-new {@link
 * RunePageState} has picked nothing yet.
 */
public record RunePageSyncPayload(
        RunePath primaryPath,
        List<ResourceLocation> primaryMinors,
        RunePath secondaryPath,
        List<ResourceLocation> secondaryMinors)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RunePageSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune_page_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunePageSyncPayload> STREAM_CODEC = StreamCodec.of(
            RunePageSyncPayload::encode, RunePageSyncPayload::decode);

    public static RunePageSyncPayload from(RunePageState state) {
        return new RunePageSyncPayload(
                state.primaryPath(),
                List.copyOf(state.primaryMinors()),
                state.secondaryPath(),
                List.copyOf(state.secondaryMinors()));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, RunePageSyncPayload payload) {
        buffer.writeVarInt(payload.primaryPath == null ? -1 : payload.primaryPath.ordinal());
        writeIds(buffer, payload.primaryMinors);
        buffer.writeVarInt(payload.secondaryPath == null ? -1 : payload.secondaryPath.ordinal());
        writeIds(buffer, payload.secondaryMinors);
    }

    private static RunePageSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int primaryOrdinal = buffer.readVarInt();
        RunePath primaryPath = primaryOrdinal < 0 ? null : RunePath.values()[primaryOrdinal];
        List<ResourceLocation> primaryMinors = readIds(buffer);
        int secondaryOrdinal = buffer.readVarInt();
        RunePath secondaryPath = secondaryOrdinal < 0 ? null : RunePath.values()[secondaryOrdinal];
        List<ResourceLocation> secondaryMinors = readIds(buffer);
        return new RunePageSyncPayload(primaryPath, primaryMinors, secondaryPath, secondaryMinors);
    }

    private static void writeIds(RegistryFriendlyByteBuf buffer, List<ResourceLocation> ids) {
        buffer.writeVarInt(ids.size());
        ids.forEach(buffer::writeResourceLocation);
    }

    private static List<ResourceLocation> readIds(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buffer.readResourceLocation());
        }
        return ids;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
