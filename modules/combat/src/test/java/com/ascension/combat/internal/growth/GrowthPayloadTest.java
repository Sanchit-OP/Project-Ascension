package com.ascension.combat.internal.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The tree screen's three payloads on the wire: what goes in comes out, and a client cannot make the
 * server read a list longer than a screen could ever send. That a packet is acted on and a player's state
 * actually reaches their screen is for a running server and client (ADR-0008).
 */
class GrowthPayloadTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ascension_combat", path);
    }

    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buffer = Unpooled.buffer();
        codec.encode(buffer, value);
        T back = codec.decode(buffer);
        assertEquals(0, buffer.readableBytes(), "the whole packet was read");
        return back;
    }

    @Test
    void aSyncComesOutAsItWentIn() {
        GrowthSyncPayload payload = new GrowthSyncPayload(
                List.of(id("guard/vitality_1"), id("power/might_1")),
                List.of(ResourceLocation.fromNamespaceAndPath("ascension_worlds", "gates/moon")),
                11, 2);
        assertEquals(payload, roundTrip(GrowthSyncPayload.STREAM_CODEC, payload));
    }

    @Test
    void anEmptyPlayerSyncs() {
        GrowthSyncPayload payload = new GrowthSyncPayload(List.of(), List.of(), 0, 0);
        assertEquals(payload, roundTrip(GrowthSyncPayload.STREAM_CODEC, payload));
    }

    @Test
    void aTakeKeepsItsOrder() {
        GrowthTakePayload payload = new GrowthTakePayload(List.of(id("guard/vitality_1"), id("guard/plating_1")));
        assertEquals(payload, roundTrip(GrowthTakePayload.STREAM_CODEC, payload));
    }

    @Test
    void aRespecIsJustTheRequest() {
        assertSame(GrowthRespecPayload.INSTANCE, roundTrip(GrowthRespecPayload.STREAM_CODEC, GrowthRespecPayload.INSTANCE));
    }

    @Test
    void aTakeLongerThanAScreenCouldSendIsRefused() {
        ByteBuf buffer = Unpooled.buffer();
        VarInt.write(buffer, GrowthTakePayload.MAX_NODES + 1);
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i <= GrowthTakePayload.MAX_NODES; i++) {
            ids.add(id("node_" + i));
        }
        ids.forEach(node -> ResourceLocation.STREAM_CODEC.encode(buffer, node));
        assertThrows(RuntimeException.class, () -> GrowthTakePayload.STREAM_CODEC.decode(buffer));
    }
}
