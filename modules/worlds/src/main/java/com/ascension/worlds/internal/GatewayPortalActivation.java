package com.ascension.worlds.internal;

import com.ascension.worlds.internal.gateway.GatewayFrame;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * What a click on a keystone does: checks the held item against {@link GatewayRules}, checks the
 * frame around the keystone against the {@link GatewayFrame}, and attunes and lights it.
 *
 * <p>The item rule and the frame check are separate on purpose: holding the right item next to an
 * incomplete frame says "the frame is incomplete" and keeps the item, rather than spending a
 * Warden tendril on nothing.
 */
final class GatewayPortalActivation {

    private GatewayPortalActivation() {
    }

    static boolean satisfies(ServerLevel level, ItemStack stack) {
        ResourceLocation portalDimension = level.dimension().location();
        return switch (GatewayRules.requirementFor(portalDimension)) {
            case WARDEN_TENDRIL -> stack.is(WorldsContent.WARDEN_TENDRIL.get());
            case PLANET_KEY -> stack.is(WorldsContent.GATEWAY_KEY.get())
                    && GatewayRules.keyOpens(stack.get(WorldsContent.GATEWAY_KEY_FOR_PLANET.get()), portalDimension);
        };
    }

    /**
     * {@code stack} already satisfies this planet's requirement. If the frame around the keystone
     * at {@code keystone} is whole, consume one, attune the keystone, light the field and register
     * the portal with the network.
     */
    static void tryAttune(ServerLevel level, BlockPos keystone, ServerPlayer player, ItemStack stack) {
        Optional<GatewayFrame.Placement> placement = findFrame(level, keystone, GatewayFrames.Opening.CLEAR);
        if (placement.isEmpty()) {
            say(player, "block.ascension_worlds.gateway_keystone.frame_incomplete", ChatFormatting.RED);
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.setBlock(keystone, level.getBlockState(keystone).setValue(GatewayKeystoneBlock.ACTIVE, true), 3);
        GatewayFrames.light(level, placement.get());
        GatewayFrames.setLit(level, placement.get(), true);
        register(level, placement.get());

        level.playSound(null, keystone, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        say(player, "block.ascension_worlds.gateway_keystone.activated", ChatFormatting.AQUA);
    }

    /** An attuned keystone: put the field back if it collapsed, then offer destinations. */
    static void relightAndOpen(ServerLevel level, BlockPos keystone, ServerPlayer player) {
        Optional<GatewayFrame.Placement> placement = findFrame(level, keystone, GatewayFrames.Opening.CLEAR_OR_FIELD);
        if (placement.isEmpty()) {
            say(player, "block.ascension_worlds.gateway_keystone.frame_incomplete", ChatFormatting.RED);
            return;
        }
        GatewayFrames.light(level, placement.get());
        GatewayFrames.setLit(level, placement.get(), true);
        register(level, placement.get());
        GatewayDestinations.sendPicker(player, level);
    }

    /**
     * An attuned keystone was broken: its frame's runes go dark. (The field collapses on its own,
     * since the keystone is part of the opening's border.) If the rest of the frame is already
     * incomplete there is nothing left to recognise, and the runes stay as they are.
     */
    static void onKeystoneRemoved(ServerLevel level, BlockPos keystone) {
        GatewayFrames.get(level.registryAccess())
                .flatMap(frame -> frame.match(keystone.getX(), keystone.getY(), keystone.getZ(),
                        GatewayFrames.testInWithoutKeystone(level, frame, GatewayFrames.Opening.CLEAR_OR_FIELD)))
                .ifPresent(placement -> GatewayFrames.setLit(level, placement, false));
    }

    /** Empty-handed, or a wrong item, on an unattuned keystone: say what it actually needs. */
    static void describeRequirement(ServerLevel level, ServerPlayer player) {
        String key = switch (GatewayRules.requirementFor(level.dimension().location())) {
            case WARDEN_TENDRIL -> "block.ascension_worlds.gateway_keystone.needs_warden_tendril";
            case PLANET_KEY -> "block.ascension_worlds.gateway_keystone.needs_gateway_key";
        };
        say(player, key, ChatFormatting.GRAY);
    }

    private static Optional<GatewayFrame.Placement> findFrame(ServerLevel level, BlockPos keystone,
                                                              GatewayFrames.Opening opening) {
        return GatewayFrames.get(level.registryAccess()).flatMap(frame -> frame.match(
                keystone.getX(), keystone.getY(), keystone.getZ(), GatewayFrames.testIn(level, frame, opening)));
    }

    /** Record this planet's portal, and where arrivals should stand, network-wide. */
    static void register(ServerLevel level, GatewayFrame.Placement placement) {
        int[] arrival = placement.arrivalSpot();
        level.getServer().overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE)
                .activate(level.dimension().location(), new BlockPos(arrival[0], arrival[1], arrival[2]));
    }

    private static void say(ServerPlayer player, String key, ChatFormatting colour) {
        player.displayClientMessage(Component.translatable(key).withStyle(colour), true);
    }
}
