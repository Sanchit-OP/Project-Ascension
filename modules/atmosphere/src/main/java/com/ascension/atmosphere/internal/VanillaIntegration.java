package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.AtmospherePriority;
import com.ascension.atmosphere.api.DrainModifier;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Folds vanilla breathing into this system.
 *
 * <p>Drowning and suffocating in vacuum are the same problem, so they should not be two meters
 * with two timers. Running them through one model also makes existing gear meaningful off-world
 * and new gear meaningful underwater, with no special cases at either end: a player kitted out
 * for the Moon is, without being told, kitted out for deep water.
 *
 * <p>Everything here honours {@link AtmosphereConfig#waterIntegrationEnabled()}, because taking
 * over a core vanilla mechanic is not something a library should do to an unsuspecting adopter.
 */
public final class VanillaIntegration {

    private VanillaIntegration() {
    }

    /**
     * Water is unbreathable, claimed at the {@code DIMENSION} band.
     *
     * <p>The low band matters: a sealed volume, a structure or a vehicle all override it without
     * writing a line of code, so a pressurised submarine simply works.
     */
    public static final class WaterAtmosphere implements AtmosphereProvider {

        @Override
        public Optional<Atmosphere> query(AtmosphereContext context) {
            if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
                return Optional.empty();
            }
            if (context.level().getFluidState(context.blockPos()).is(FluidTags.WATER)) {
                return Optional.of(Atmosphere.VACUUM);
            }
            return Optional.empty();
        }

        @Override
        public int priority() {
            return AtmospherePriority.DIMENSION;
        }
    }

    /**
     * Maps vanilla breathing gear onto the drain model.
     *
     * <p>Expressed as a {@link DrainModifier} rather than a new API concept, because that is
     * exactly what the registry is for: something that changes how fast you use air.
     *
     * <ul>
     *   <li>Conduit power returns {@code 0} &mdash; nothing drains, so nothing suffocates.</li>
     *   <li>Respiration and a turtle helmet reduce consumption rather than adding a separate
     *       timer, so they help in vacuum too.</li>
     * </ul>
     */
    public static final class VanillaBreathingGear implements DrainModifier {

        private static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "vanilla_gear");

        /** Each Respiration level cuts consumption by this fraction of the base. */
        private static final float RESPIRATION_PER_LEVEL = 0.25f;

        /** A turtle helmet is worth roughly one Respiration level. */
        private static final float TURTLE_HELMET_BONUS = 0.25f;

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public float multiplier(ServerPlayer player) {
            if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
                return 1.0f;
            }
            if (player.hasEffect(MobEffects.CONDUIT_POWER)) {
                return 0.0f;
            }

            float reduction = 0.0f;

            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var respiration = registry.getOrThrow(Enchantments.RESPIRATION);
            int level = player.getItemBySlot(EquipmentSlot.HEAD).getEnchantmentLevel(respiration);
            reduction += level * RESPIRATION_PER_LEVEL;

            if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET)) {
                reduction += TURTLE_HELMET_BONUS;
            }

            // Never quite free: fully cancelling drain from gear alone would make the whole
            // preparation loop skippable with a single enchantment.
            return Math.max(0.15f, 1.0f - reduction);
        }
    }
}
