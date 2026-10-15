package com.ascension.combat.api.stat;

import com.ascension.combat.api.stat.CombatStat.Format;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The stats of the combat stat model (docs/technical/item-stats.md section 3), owned by the
 * item-stats work. Another system that needs a new stat asks for it in combat-api.md section 3,
 * "Requested stats", rather than registering an attribute of its own.
 *
 * <p>Four live on vanilla attributes, so every mod's weapon or potion already feeds them; the
 * other six are this module's own ({@code internal.CombatAttributes}).
 *
 * <p>Attack Speed and Move Speed bonuses add up before they scale the speed
 * ({@link Operation#ADD_MULTIPLIED_BASE}): two +20% bonuses make +40%, as League's do.
 */
public final class CombatStats {

    private static final Map<String, CombatStat> BY_NAME = new LinkedHashMap<>();

    public static final CombatStat HEALTH =
            stat("health", Format.FLAT, Operation.ADD_VALUE, () -> Attributes.MAX_HEALTH);
    public static final CombatStat ATTACK_DAMAGE =
            stat("attack_damage", Format.FLAT, Operation.ADD_VALUE, () -> Attributes.ATTACK_DAMAGE);
    public static final CombatStat ATTACK_SPEED =
            stat("attack_speed", Format.PERCENT, Operation.ADD_MULTIPLIED_BASE, () -> Attributes.ATTACK_SPEED);
    public static final CombatStat MOVE_SPEED =
            stat("move_speed", Format.PERCENT, Operation.ADD_MULTIPLIED_BASE, () -> Attributes.MOVEMENT_SPEED);
    public static final CombatStat ARMOR = stat("armor", Format.FLAT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.ARMOR);
    public static final CombatStat MAGIC_RESIST = stat("magic_resist", Format.FLAT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.MAGIC_RESIST);
    public static final CombatStat OMNIVAMP = stat("omnivamp", Format.PERCENT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.OMNIVAMP);
    public static final CombatStat LETHALITY = stat("lethality", Format.FLAT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.LETHALITY);
    public static final CombatStat ARMOR_PENETRATION = stat("armor_penetration", Format.PERCENT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.ARMOR_PENETRATION_PERCENT);
    public static final CombatStat MAGIC_PENETRATION = stat("magic_penetration", Format.PERCENT, Operation.ADD_VALUE,
            () -> com.ascension.combat.internal.CombatAttributes.MAGIC_PENETRATION_PERCENT);

    /** A stat by its name in data. An unknown name fails to decode rather than being skipped. */
    public static final Codec<CombatStat> CODEC = Codec.STRING.comapFlatMap(
            name -> byName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown combat stat '" + name + "', expected one of " + BY_NAME.keySet())),
            CombatStat::name);

    private CombatStats() {
    }

    public static Optional<CombatStat> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** Every stat, in the order section 3 lists them. */
    public static List<CombatStat> all() {
        return List.copyOf(BY_NAME.values());
    }

    private static CombatStat stat(String name, Format format, Operation operation, Supplier<Holder<Attribute>> attribute) {
        CombatStat stat = new CombatStat(name, attribute, format, operation);
        BY_NAME.put(name, stat);
        return stat;
    }
}
