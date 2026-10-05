package com.ascension.worlds.internal;

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
 * Applies {@link GatewayRules} to a real click on a portal: checks the held item, consumes it, and
 * flips this planet's network-wide flag. The rules themselves (what each planet's portal needs)
 * live in {@link GatewayRules}, so the check and the "what does this need?" hint below can never
 * disagree.
 */
final class GatewayPortalActivation {

    private GatewayPortalActivation() {
    }

    /**
     * If {@code stack} satisfies this planet's own requirement, consumes one and activates the
     * network-wide flag for this planet. Returns whether it was satisfied &mdash; not whether the
     * network flag actually changed, since re-activating an already-active portal with the right
     * item should still feel like it worked rather than silently failing.
     */
    static boolean tryActivate(ServerLevel level, BlockPos portal, ServerPlayer player, ItemStack stack) {
        ResourceLocation planetId = level.dimension().location();
        if (!satisfies(planetId, stack)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ServerLevel overworld = level.getServer().overworld();
        boolean changed = overworld.getData(SpaceAttachments.GATEWAY_NETWORK_STATE).activate(planetId, portal);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (changed) {
            player.displayClientMessage(
                    Component.translatable("block.ascension_worlds.gateway_portal.activated")
                            .withStyle(ChatFormatting.AQUA),
                    true);
        }
        return true;
    }

    /** Empty-handed, or a wrong item, on an inactive portal: say what it actually needs. */
    static void describeRequirement(ServerLevel level, ServerPlayer player) {
        String key = switch (GatewayRules.requirementFor(level.dimension().location())) {
            case RELAY_COIL -> "block.ascension_worlds.gateway_portal.needs_relay_coil";
            case PLANET_KEY -> "block.ascension_worlds.gateway_portal.needs_gateway_key";
        };
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    private static boolean satisfies(ResourceLocation portalDimension, ItemStack stack) {
        return switch (GatewayRules.requirementFor(portalDimension)) {
            case RELAY_COIL -> stack.is(WorldsContent.RELAY_COIL.get());
            case PLANET_KEY -> stack.is(WorldsContent.GATEWAY_KEY.get())
                    && GatewayRules.keyOpens(stack.get(WorldsContent.GATEWAY_KEY_FOR_PLANET.get()), portalDimension);
        };
    }
}
