package com.ascension.atmosphere.internal;

import com.ascension.atmosphere.AscensionAtmosphere;
import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmosphereProvider;
import com.ascension.atmosphere.api.AtmospherePriority;
import com.ascension.atmosphere.api.DrainModifier;
import com.ascension.atmosphere.api.LungCapacityModifier;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
     * Conduit power stops drain entirely.
     *
     * <p>A drain modifier rather than lung capacity, because the effect is not "bigger lungs" —
     * it is "nothing is consuming your air". Returning zero means nothing drains, and since
     * suffocation triggers on drain rather than on breathability, nothing suffocates either.
     *
     * <p>Without this, a player with a fully built conduit — an expensive endgame item whose
     * entire purpose is underwater breathing — would start drowning in their own base the moment
     * this mod took over water.
     */
    public static final class ConduitPower implements DrainModifier {

        private static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "conduit_power");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public float multiplier(ServerPlayer player) {
            if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
                return 1.0f;
            }
            return player.hasEffect(MobEffects.CONDUIT_POWER) ? 0.0f : 1.0f;
        }
    }

    /**
     * Respiration and turtle helmets make your lungs bigger.
     *
     * <p>Deliberately capacity rather than a drain reduction. Both would make a full bar last
     * longer, but a drain reduction slows consumption of <em>everything</em>, so a helmet
     * enchantment would quietly stretch a carried tank's duration too. Growing lungs keeps the
     * two supplies independent: gear improves the free, self-refilling reserve, and a tank stays
     * exactly as big as the tank you built.
     *
     * <p>It also keeps the HUD honest. The bar is always the same width; better gear simply means
     * a full bar is worth more seconds.
     */
    public static final class VanillaBreathingGear implements LungCapacityModifier {

        private static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(AscensionAtmosphere.MOD_ID, "vanilla_gear");

        /** Seconds of extra lung capacity per level of Respiration. */
        private static final int RESPIRATION_SECONDS_PER_LEVEL = 10;

        /** Seconds of extra lung capacity from a turtle helmet. */
        private static final int TURTLE_HELMET_SECONDS = 10;

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public int bonusUnits(ServerPlayer player) {
            if (!AtmosphereConfig.INSTANCE.waterIntegrationEnabled()) {
                return 0;
            }
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.isEmpty()) {
                return 0;
            }

            int seconds = 0;

            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int respiration = helmet.getEnchantmentLevel(registry.getOrThrow(Enchantments.RESPIRATION));
            seconds += respiration * RESPIRATION_SECONDS_PER_LEVEL;

            if (helmet.is(Items.TURTLE_HELMET)) {
                seconds += TURTLE_HELMET_SECONDS;
            }

            return seconds * AtmosphereTuning.BASE_UNITS_PER_SECOND;
        }
    }
}
