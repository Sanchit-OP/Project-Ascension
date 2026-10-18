package com.ascension.combat.internal.mechanics;

import com.ascension.combat.AscensionCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Whether one hit is physical or magic (docs/technical/damage-kinds.md).
 *
 * <p>The rule is about the <em>attack</em>, not the mob: a Warden's fist is physical and its sonic
 * boom is magic, a Creeper's blast is magic, a skeleton's arrow is physical. Minecraft already
 * names every attack by a damage type, so the answer is read from two tags, and both are plain data
 * any mod or pack can extend:
 * <ul>
 *   <li>{@link #MAGIC}, a damage-type tag: attacks of these types are magic (blasts, fire,
 *       lightning, potions and curses, the Wither's decay, the dragon's breath, the Warden's
 *       boom).</li>
 *   <li>{@link #MAGIC_ATTACK}, an entity-type tag: a hit whose <em>direct</em> cause is one of these
 *       entities is magic even when it carries a generic type. This is for mods that give every
 *       projectile {@code mob_projectile} (Cataclysm's fireballs and orbs). Melee has no direct
 *       entity other than the attacker, so a mob's own swing is always judged by its damage type.</li>
 * </ul>
 * Everything else is physical, which is the common case (combat-api.md section 2). Damage no one
 * dealt on purpose (falling, drowning, starving, the void, suffocation) is in neither tag and is
 * untouched by either defence, as it was before.
 *
 * <p>{@link PhysicalDamage} reads the answer: it cuts a magic hit by the target's Magic Resist
 * where it used to cut every hit by Armor.
 */
public final class DamageKinds {

    /** Damage types that are magic. {@code data/ascension_combat/tags/damage_type/magic.json}. */
    public static final TagKey<DamageType> MAGIC =
            TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "magic"));

    /** Entities whose hits are magic. {@code data/ascension_combat/tags/entity_type/magic_attack.json}. */
    public static final TagKey<EntityType<?>> MAGIC_ATTACK =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "magic_attack"));

    private DamageKinds() {
    }

    /** True when {@code source} is a magic hit; false means physical (or nobody's doing). */
    public static boolean isMagic(DamageSource source) {
        if (source.is(MAGIC)) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        // A direct entity that is the attacker itself is a melee swing: judged by its type alone.
        return direct != null && direct != source.getEntity() && direct.getType().is(MAGIC_ATTACK);
    }
}
