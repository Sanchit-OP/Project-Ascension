package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A player's growth, for their own tree screen (docs/progression/growth.md section 9, "Sync"): the
 * nodes they have taken, in the order they took them, the milestones they have earned, and the points
 * earned and spent. Sent at login and whenever {@link PlayerGrowth#refresh} runs, which is every event
 * that can change any of it, and never per tick.
 *
 * <p>Ids only, and two numbers. What a node gives, what a milestone is worth and what an advancement
 * is called are already on the client: both growth registries are synced datapack registries, and the
 * advancements are synced by vanilla.
 *
 * @param taken nodes taken, in order
 * @param milestones the advancement ids of the milestones earned
 * @param earned points earned
 * @param spent points spent on nodes the data still defines
 */
public record GrowthSyncPayload(List<ResourceLocation> taken, List<ResourceLocation> milestones, int earned, int spent)
        implements CustomPacketPayload {

    /** Far more than any tree or list of milestones will hold; bounds what a packet may claim. */
    private static final int MAX_IDS = 1024;

    public static final CustomPacketPayload.Type<GrowthSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "growth_sync"));

    public static final StreamCodec<ByteBuf, GrowthSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_IDS)), GrowthSyncPayload::taken,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_IDS)), GrowthSyncPayload::milestones,
            ByteBufCodecs.VAR_INT, GrowthSyncPayload::earned,
            ByteBufCodecs.VAR_INT, GrowthSyncPayload::spent,
            GrowthSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
