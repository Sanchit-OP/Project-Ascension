package com.ascension.combat.internal.mobs;

import com.ascension.combat.internal.CombatAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Gives vanilla mobs a place in this module's stat model (docs/technical/combat-api.md section 3)
 * &mdash; {@link CombatAttributes#ARMOR} and {@link CombatAttributes#MAGIC_RESIST}, this module's
 * own attributes, not vanilla's. The damage pipeline already treats mobs exactly like players
 * everywhere else (every magic-damage proc &mdash; Ragefang, Darken Blade, Heartwood of Pallas,
 * Divine Sunderer &mdash; already resolves its target as a generic {@link LivingEntity}, never a
 * {@code Player}; {@link com.ascension.combat.internal.mechanics.PhysicalDamage} reduces the same
 * way for every target), but nothing had ever assigned mobs a value under either attribute, so
 * every hit against wildlife had nothing to cut through.
 *
 * <p>{@link CombatAttributes#onAttributeModification} runs first (registered earlier in {@code
 * AscensionCombat}'s constructor) and gives every living entity type both attributes at their bare
 * default of 0; this class registers immediately after it and overrides both for the mobs actually
 * worth distinguishing &mdash; {@link EntityAttributeModificationEvent#add} is last-write-wins per
 * entity type and attribute, so registration order here is load-bearing.
 *
 * <p>HP and Attack Damage are untouched: those already vary meaningfully per vanilla mob (a
 * Zombie's 20 HP versus a Ravager's 100 is already a real difference), unlike Armor and Magic
 * Resist, which had no presence in this stat model at all until now.
 *
 * <p>Scope: every vanilla hostile mob across the Overworld, Nether, and End, plus the handful of
 * neutral mobs that actually fight (Wolf, Polar Bear, Bee, Iron Golem). Pure passive mobs (cows,
 * pigs, chickens, horses, and similar) are left out entirely &mdash; they never take part in
 * combat, so a value for one would never be read by anything.
 */
public final class MobStats {

    private MobStats() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(MobStats::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        tier(event, MobStatTier.SWARM,
                EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.VEX);

        tier(event, MobStatTier.LIGHT,
                EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
                EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED,
                EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.CREEPER,
                EntityType.WITCH, EntityType.SLIME, EntityType.MAGMA_CUBE,
                EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.PHANTOM,
                EntityType.WOLF, EntityType.BEE);

        tier(event, MobStatTier.MEDIUM,
                EntityType.ENDERMAN, EntityType.BLAZE, EntityType.GHAST,
                EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOGLIN,
                EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.EVOKER, EntityType.ILLUSIONER,
                EntityType.GUARDIAN, EntityType.SHULKER, EntityType.WITHER_SKELETON,
                EntityType.BREEZE, EntityType.POLAR_BEAR);

        tier(event, MobStatTier.HEAVY,
                EntityType.RAVAGER, EntityType.ELDER_GUARDIAN, EntityType.IRON_GOLEM);

        tier(event, MobStatTier.BOSS,
                EntityType.WARDEN, EntityType.WITHER, EntityType.ENDER_DRAGON);
    }

    @SafeVarargs
    private static void tier(
            EntityAttributeModificationEvent event, MobStatTier tier, EntityType<? extends LivingEntity>... types) {
        for (EntityType<? extends LivingEntity> type : types) {
            event.add(type, CombatAttributes.ARMOR, tier.armor());
            event.add(type, CombatAttributes.MAGIC_RESIST, tier.magicResist());
        }
    }
}
