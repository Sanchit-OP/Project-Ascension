package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "Save my rune page as this" (docs/technical/combat-api.md section 7) &mdash; sent client to
 * server when the player presses Confirm on {@link com.ascension.combat.client.RuneScreen}. Sent
 * once on confirm, not per click inside the screen: ADR-0007 rule 8 asks for no per-tick packets,
 * and every intermediate click while building a page is purely local UI state until the player
 * actually commits to it.
 *
 * <p>The server-side handler ({@link CombatNetwork#onSetRunePage}) does not trust this payload's
 * shape directly &mdash; it hands the fields to {@link com.ascension.combat.internal.rune.RunePageState#trySetPage}
 * for validation against {@link com.ascension.combat.internal.rune.RuneCatalog} before writing
 * anything, the same "server re-derives, never trusts the client's own claim" posture every other
 * C2S payload in this module already follows.
 *
 * <p>{@code secondaryPath} is nullable (encoded as ordinal {@code -1}) &mdash; a page can have a
 * primary path chosen with no secondary yet.
 */
public record SetRunePagePayload(
        RunePath primaryPath,
        List<ResourceLocation> primaryMinors,
        RunePath secondaryPath,
        List<ResourceLocation> secondaryMinors)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetRunePagePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "set_rune_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetRunePagePayload> STREAM_CODEC = StreamCodec.of(
            SetRunePagePayload::encode, SetRunePagePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SetRunePagePayload payload) {
        buffer.writeVarInt(payload.primaryPath.ordinal());
        writeIds(buffer, payload.primaryMinors);
        buffer.writeVarInt(payload.secondaryPath == null ? -1 : payload.secondaryPath.ordinal());
        writeIds(buffer, payload.secondaryMinors);
    }

    private static SetRunePagePayload decode(RegistryFriendlyByteBuf buffer) {
        RunePath primaryPath = RunePath.values()[buffer.readVarInt()];
        List<ResourceLocation> primaryMinors = readIds(buffer);
        int secondaryOrdinal = buffer.readVarInt();
        RunePath secondaryPath = secondaryOrdinal < 0 ? null : RunePath.values()[secondaryOrdinal];
        List<ResourceLocation> secondaryMinors = readIds(buffer);
        return new SetRunePagePayload(primaryPath, primaryMinors, secondaryPath, secondaryMinors);
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
