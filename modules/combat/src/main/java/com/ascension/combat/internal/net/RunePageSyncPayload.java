package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageRules;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's saved rune page as their own client needs it to draw the screen
 * (docs/technical/rune-system.md section 9): the page, how far each growing rune has grown, which
 * slots are open, and what a change costs. Sent on login, after every save, and when growth moves.
 * Never per tick.
 *
 * <p>The runes themselves (names, numbers, rows) are not here: the rune registry is synced to
 * every client by itself.
 */
public record RunePageSyncPayload(
        RunePage page,
        Map<ResourceLocation, Integer> growth,
        RunePageRules.OpenSlots open,
        int chalk,
        int levels)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RunePageSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune_page_sync"));

    private static final StreamCodec<ByteBuf, RunePageRules.OpenSlots> OPEN = StreamCodec.composite(
            ByteBufCodecs.BOOL, RunePageRules.OpenSlots::secondary,
            ByteBufCodecs.BOOL, RunePageRules.OpenSlots::offence,
            ByteBufCodecs.BOOL, RunePageRules.OpenSlots::flex,
            ByteBufCodecs.BOOL, RunePageRules.OpenSlots::defence,
            RunePageRules.OpenSlots::new);

    private static final StreamCodec<ByteBuf, Map<ResourceLocation, Integer>> GROWTH =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT, 8);

    public static final StreamCodec<ByteBuf, RunePageSyncPayload> STREAM_CODEC = StreamCodec.composite(
            RunePage.STREAM_CODEC, RunePageSyncPayload::page,
            GROWTH, RunePageSyncPayload::growth,
            OPEN, RunePageSyncPayload::open,
            ByteBufCodecs.VAR_INT, RunePageSyncPayload::chalk,
            ByteBufCodecs.VAR_INT, RunePageSyncPayload::levels,
            RunePageSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
