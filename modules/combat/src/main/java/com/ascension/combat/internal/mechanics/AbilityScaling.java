package com.ascension.combat.internal.mechanics;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * How the legendaries' abilities grow with their wielder's stats (docs/progression/balance.md
 * section 4): each coefficient is "extra per point of one stat", read as a total through
 * {@link StatReader}, never from an attribute directly. One class holds every coefficient, so the
 * whole scaling sheet is tuned in one place. The base numbers stay in each effects class.
 *
 * <p>Placeholders Sanchit tunes by playing; the tooltips ({@code tooltip.ascension_combat.*}) state
 * the base number and say what the ability scales with.
 */
public final class AbilityScaling {

    /** Veigo's Blade: extra share of the target's current health per point of Attack Damage (0.2%). */
    public static final float VEIGO_CURRENT_HP_PER_AD = 0.002f;
    /** Darken Blade: extra share of the target's max health per Attack Damage (0.1%), and shield per point. */
    public static final float DARKEN_MAX_HP_PER_AD = 0.001f;
    public static final float DARKEN_SHIELD_PER_AD = 0.5f;
    /** Divine Sunderer: extra share of max health per Attack Damage, for each of its two halves (0.1%). */
    public static final float SUNDERER_MAX_HP_PER_AD = 0.001f;
    /** Spear of Shojin: extra share of max health, as true damage, per Attack Damage (0.1%). */
    public static final float SHOJIN_MAX_HP_PER_AD = 0.001f;
    /** Galeforce: extra share of the target's missing health per Attack Damage (0.5%). */
    public static final float GALEFORCE_MISSING_HP_PER_AD = 0.005f;
    /** Heartwood of Pallas and Ragefang: extra flat magic damage per Attack Damage on each hit. */
    public static final float HEARTWOOD_ON_HIT_PER_AD = 0.15f;
    public static final float RAGEFANG_ON_HIT_PER_AD = 0.2f;
    /** Rocketbelt: extra burst damage per Attack Damage. */
    public static final float ROCKETBELT_BURST_PER_AD = 0.5f;
    /** Unending Despair: extra pulse damage per point of Health (10%). */
    public static final float DESPAIR_PULSE_PER_HEALTH = 0.1f;
    /** Gargoyle Stoneplate: extra shield per nearby enemy per point of Armor, and extra cap per Armor. */
    public static final float GARGOYLE_PER_ENEMY_PER_ARMOR = 0.05f;
    public static final float GARGOYLE_CAP_PER_ARMOR = 0.4f;

    private AbilityScaling() {
    }

    /** The Attack Damage of whoever dealt a hit, 0 for anything that is not a living entity. */
    public static float attackDamage(@Nullable Entity entity) {
        return stat(entity, CombatStats.ATTACK_DAMAGE);
    }

    public static float stat(@Nullable Entity entity, CombatStat stat) {
        return entity instanceof LivingEntity living ? (float) StatReader.value(living, stat) : 0.0f;
    }
}
