package com.ascension.combat.internal.net;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.rune.RunePageRules;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The answer to a {@link SetRunePagePayload}: saved, or refused and why
 * (docs/technical/rune-system.md section 7: "saving is refused by the server with that reason, not
 * silently"). The screen shows the reason and keeps the player's unsaved choices.
 */
public record RunePageResultPayload(Optional<RunePageRules.Refusal> refusal) implements CustomPacketPayload {

    public static final RunePageResultPayload SAVED = new RunePageResultPayload(Optional.empty());

    public static final CustomPacketPayload.Type<RunePageResultPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune_page_result"));

    private static final StreamCodec<ByteBuf, RunePageRules.Refusal> REFUSAL =
            ByteBufCodecs.idMapper(i -> RunePageRules.Refusal.values()[i], RunePageRules.Refusal::ordinal);

    public static final StreamCodec<ByteBuf, RunePageResultPayload> STREAM_CODEC =
            ByteBufCodecs.optional(REFUSAL).map(RunePageResultPayload::new, RunePageResultPayload::refusal);

    public static RunePageResultPayload refused(RunePageRules.Refusal why) {
        return new RunePageResultPayload(Optional.of(why));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
