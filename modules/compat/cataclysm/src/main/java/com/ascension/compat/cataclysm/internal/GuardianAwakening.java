package com.ascension.compat.cataclysm.internal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Spawns the Ender Guardian where the Ender Golem just died.
 *
 * <p>Nothing in Cataclysm's own data or Java spawns the Guardian at all &mdash; confirmed by
 * decompiling the whole Ruined Citadel piece (only "sentry", "mimic" and "golem" data markers
 * exist) and scanning every datapack json in the mod for the string {@code ender_guardian}
 * outside its own loot table and advancement (nothing). In vanilla Cataclysm the Guardian is
 * evidently reached some other way this project doesn't need to reverse-engineer further: a
 * mandatory progression gate needs a guaranteed trigger, not a maybe. Golem death is the obvious
 * one, and matches the boss descriptions' own framing of the golem as guarding the citadel the
 * Guardian is the true final boss of.
 *
 * <p>By registry id, not Cataclysm's Java &mdash; same posture as everything else in this module.
 */
public final class GuardianAwakening {

    private static final ResourceLocation ENDER_GOLEM = ResourceLocation.fromNamespaceAndPath("cataclysm", "ender_golem");
    private static final ResourceLocation ENDER_GUARDIAN = ResourceLocation.fromNamespaceAndPath("cataclysm", "ender_guardian");

    private GuardianAwakening() {
    }

    /**
     * {@code LOWEST}, and canceled deaths skipped (the default): {@code LivingDeathEvent} is
     * cancelable, and a Golem some other mod revives must not leave a Guardian behind it.
     */
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, GuardianAwakening::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(ENDER_GOLEM)) {
            return;
        }

        EntityType<?> guardianType = BuiltInRegistries.ENTITY_TYPE.get(ENDER_GUARDIAN);
        Entity guardian = guardianType.create(level);
        if (guardian == null) {
            return;
        }
        guardian.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0.0f);
        level.addFreshEntity(guardian);
    }
}
