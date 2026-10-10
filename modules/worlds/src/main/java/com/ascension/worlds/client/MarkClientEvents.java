package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Where the mark meets the renderers: its layers go onto the player and illager renderers once, at
 * setup; the first-person hand gets it every frame the empty hand is drawn; and the gate progress
 * behind it is forgotten on logout.
 *
 * <p>No mixins (ADR-0003): layers through NeoForge's {@code AddLayers}, and the first-person hand
 * through {@code RenderArmEvent}, which is never cancelled here so other mods' arms keep working.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class MarkClientEvents {

    /** Every renderer drawing {@code IllagerModel}'s crossed arms. */
    private static final List<EntityType<? extends AbstractIllager>> ILLAGERS =
            List.of(EntityType.VINDICATOR, EntityType.EVOKER, EntityType.PILLAGER, EntityType.ILLUSIONER);

    private MarkClientEvents() {
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new PlayerMarkLayer(player, model == PlayerSkin.Model.SLIM));
            }
        }
        for (EntityType<? extends AbstractIllager> type : ILLAGERS) {
            EntityRenderer<?> renderer = event.getRenderer(type);
            if (renderer instanceof IllagerRenderer<?> illager) {
                addIllagerLayer(illager);
            }
        }
    }

    private static <T extends AbstractIllager> void addIllagerLayer(IllagerRenderer<T> renderer) {
        renderer.addLayer(new IllagerMarkLayer<>(renderer));
    }

    /**
     * The mark on the first-person hand. This event fires inside {@code renderRightHand}, just
     * before vanilla poses and draws the arm, so the arm here gets the pose vanilla is about to give
     * it (its own {@code renderHand}: no attack, crouch or swim, {@code setupAnim} with zeros, then
     * {@code xRot} 0, with an empty hand's pose). Vanilla then sets the same fields itself and draws
     * the arm over the same spot, with the decal 0.3 px proud of it.
     *
     * <p>Vanilla draws no arm at all while an item is held, so the mark shows in first person only
     * with an empty hand, and only on the right arm, where it lives.
     */
    @SubscribeEvent
    static void onRenderArm(RenderArmEvent event) {
        int stage = ClientGateProgress.markStage();
        if (stage == 0 || event.getArm() != HumanoidArm.RIGHT) {
            return;
        }
        AbstractClientPlayer player = event.getPlayer();
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        model.rightArm.xRot = 0.0F;

        PlayerMarkLayer.draw(event.getPoseStack(), event.getMultiBufferSource(), model.rightArm,
                player.getSkin().model() == PlayerSkin.Model.SLIM, stage, player, event.getPackedLight());
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGateProgress.clear();
    }
}
