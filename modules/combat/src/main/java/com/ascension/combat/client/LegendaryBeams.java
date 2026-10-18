package com.ascension.combat.client;

import com.ascension.combat.AscensionCombat;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * A column of light over a dropped legendary item, so one lying in a cave or a field is seen from
 * across the room, as a beacon's beam is. Vanilla's own beam renderer, in gold.
 *
 * <p>Event-driven, with no world scan: the ids of item entities are kept as they join and leave the
 * client level (plain ints, never the entities, per ADR-0007), and each frame only those are
 * resolved, drawing a beam where the stack is in the {@code ascension_combat:legendary} tag. The
 * set is emptied on logout, and an id whose entity is gone is dropped the first time it is looked up.
 */
public final class LegendaryBeams {

    private static final TagKey<Item> LEGENDARY = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "legendary"));
    private static final int GOLD = 0xFFFFC840;
    private static final int HEIGHT = 10;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;

    /** Entity ids of item entities in the client level. */
    private static final Set<Integer> ITEM_IDS = new HashSet<>();

    private LegendaryBeams() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(LegendaryBeams::onJoin);
        bus.addListener(LegendaryBeams::onLeave);
        bus.addListener(LegendaryBeams::onLogout);
        bus.addListener(LegendaryBeams::onRender);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity) {
            ITEM_IDS.add(event.getEntity().getId());
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity) {
            ITEM_IDS.remove(event.getEntity().getId());
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ITEM_IDS.clear();
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ITEM_IDS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        boolean drew = false;

        for (Iterator<Integer> it = ITEM_IDS.iterator(); it.hasNext(); ) {
            if (!(minecraft.level.getEntity(it.next()) instanceof ItemEntity item) || item.isRemoved()) {
                it.remove();
                continue;
            }
            if (!item.getItem().is(LEGENDARY) || item.distanceToSqr(camera) > MAX_DISTANCE_SQR) {
                continue;
            }
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(Mth.lerp(partial, item.xo, item.getX()) - camera.x,
                    Mth.lerp(partial, item.yo, item.getY()) - camera.y,
                    Mth.lerp(partial, item.zo, item.getZ()) - camera.z);
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partial, 1.0f,
                    minecraft.level.getGameTime(), 0, HEIGHT, GOLD, 0.15f, 0.2f);
            pose.popPose();
            drew = true;
        }
        if (drew) {
            buffers.endBatch();
        }
    }
}
