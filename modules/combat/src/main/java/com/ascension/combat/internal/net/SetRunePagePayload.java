package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.RunePage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "Save my rune page as this" (docs/technical/rune-system.md section 9), sent client to server
 * when the player presses Save. Once per save, never per click: ADR-0007 asks for no chatty
 * packets, and every click before Save is the screen's own business.
 *
 * <p>The server trusts nothing in it: {@link com.ascension.combat.internal.rune.Runes#save} runs
 * the page through the rules, combat and the cost before writing anything, and answers with a
 * {@link RunePageResultPayload}.
 */
public record SetRunePagePayload(RunePage page) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetRunePagePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "set_rune_page"));

    public static final StreamCodec<ByteBuf, SetRunePagePayload> STREAM_CODEC =
            RunePage.STREAM_CODEC.map(SetRunePagePayload::new, SetRunePagePayload::page);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
