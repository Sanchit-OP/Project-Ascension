package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What activates a planet's own gateway portal &mdash; one branch per planet, the same "frugal in
 * JSON, generous in Java" shape {@link com.ascension.worlds.api.Planet.Weather}'s own {@code type}
 * dispatch already uses (see its javadoc). Earth needs a small crafted cost (Sanchit's call,
 * 2026-09-26: not free, but not a boss either); the Moon reuses the boss/key chain M3.1 already
 * built. Planet 3 needs a third branch here once it exists; nothing about this needs a provider
 * registry until a third case exists to show what the real seam is.
 */
final class GatewayPortalActivation {

    private static final ResourceKey<Level> MOON = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "moon"));

    private GatewayPortalActivation() {
    }

    /**
     * If {@code stack} satisfies this planet's own requirement, consumes one and activates the
     * network-wide flag for this planet. Returns whether it was satisfied &mdash; not whether the
     * network flag actually changed, since re-activating an already-active portal with the right
     * item should still feel like it worked rather than silently failing.
     */
    static boolean tryActivate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!satisfies(level, stack)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ResourceLocation planetId = level.dimension().location();
        ServerLevel overworld = level.getServer().overworld();
        boolean changed = overworld.getData(SpaceAttachments.GATEWAY_NETWORK_STATE).activate(planetId);
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
        String key = level.dimension().equals(Level.OVERWORLD)
                ? "block.ascension_worlds.gateway_portal.needs_relay_coil"
                : "block.ascension_worlds.gateway_portal.needs_gateway_key";
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    private static boolean satisfies(ServerLevel level, ItemStack stack) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return stack.is(WorldsContent.RELAY_COIL.get());
        }
        if (level.dimension().equals(MOON)) {
            ResourceLocation forPlanet = stack.get(WorldsContent.GATEWAY_KEY_FOR_PLANET.get());
            return stack.is(WorldsContent.GATEWAY_KEY.get()) && MOON.location().equals(forPlanet);
        }
        return false;
    }
}
