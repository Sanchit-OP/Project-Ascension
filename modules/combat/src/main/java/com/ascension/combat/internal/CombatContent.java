package com.ascension.combat.internal;

import com.ascension.combat.AscensionCombat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything this module puts into a game registry.
 *
 * <p>One class rather than several, matching {@code ascension-atmosphere}'s own convention: the
 * registries here are small, and the interesting question for a reader is "what does this mod
 * add", not "which registry did it go in".
 */
public final class CombatContent {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AscensionCombat.MOD_ID);

    /** One per armor item; see {@link #armor}. Declared before the items, which register into it. */
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AscensionCombat.MOD_ID);

    /**
     * The first item out the door (docs/technical/combat-api.md section 6). Its on-hit bonus is
     * not an attribute carried here &mdash; it is applied by
     * {@link com.ascension.combat.internal.weapons.VeigosBladeEffects}, which checks for this
     * exact item in the attacker's main hand.
     *
     * <p>Base stats still borrow {@code Tiers.DIAMOND} as a stand-in &mdash; combat-api.md
     * section 3 is explicit that every number here is a placeholder Sanchit will tune by playing,
     * not by spec. The item uses a custom cuboid model with a 64x64 teal metal/leather atlas.
     * Its editable Blockbench source lives in design/weapons/viegos-blade.
     */
    public static final Supplier<Item> VEIGOS_BLADE = ITEMS.register(
            "veigos_blade",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3.0f, -2.4f))));

    /**
     * Second out the door, and half of the anti-tank family with {@link #VEIGOS_BLADE}
     * (combat-api.md section 6). Modeled as a {@link SwordItem} for now despite the name &mdash;
     * the "axe" identity is flavor, not a chop/strip moveset, and every other weapon in this
     * catalog is a reskinned sword until there's a reason not to be. The stacking armor shred is
     * applied by {@link com.ascension.combat.internal.weapons.DariusAxeEffects}, not carried as
     * an attribute here. Base stats are placeholders; the art (the Black Cleaver, Sanchit's call
     * 2026-10-06) is drawn by tools/generate-combat-art.py, like every item here.
     */
    public static final Supplier<Item> DARIUS_AXE = ITEMS.register(
            "darius_axe",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4.0f, -3.0f))));

    /**
     * Third out the door (combat-api.md section 6). No ability haste in this game, so the
     * original's cooldown-reduction identity is reworked entirely: consecutive on-hit strikes
     * build a combo counter that resolves as a burst of true damage &mdash; the one weapon in
     * the catalog that deals the third damage type (section 2), which bypasses armor and magic
     * resist both. Applied by
     * {@link com.ascension.combat.internal.weapons.SpearOfShojinEffects}. Modeled as a
     * {@link SwordItem} for now, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> SPEAR_OF_SHOJIN = ITEMS.register(
            "spear_of_shojin",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3.0f, -2.6f))));

    /**
     * Fourth out the door, and the first ranged weapon (combat-api.md section 6). A plain
     * {@link BowItem} &mdash; its arrows fly and land exactly like vanilla arrows, since the
     * identity here is entirely the on-hit proc, not the shot itself. Applied by
     * {@link com.ascension.combat.internal.weapons.MortalReminderEffects}, which reads
     * {@code DamageSource.getWeaponItem()} rather than the shooter's current main hand &mdash;
     * vanilla already tracks which bow fired an in-flight arrow, which matters here since the
     * player could swap items before the arrow lands. No art yet, same placeholder reasoning as
     * {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> MORTAL_REMINDER = ITEMS.register(
            "mortal_reminder",
            () -> new BowItem(new Item.Properties().durability(384)));

    /**
     * Fifth out the door, and the last piece of the anti-tank family alongside
     * {@link #VEIGOS_BLADE}, {@link #DARIUS_AXE} and {@link #MORTAL_REMINDER} (combat-api.md
     * section 6): current-HP%, armor-shred, armor-ignore, and now max-HP%, the fourth distinct
     * way this stat model cuts through a tank. The first weapon with an active rather than a pure
     * on-hit proc &mdash; right-click primes the next attack, on a cooldown tracked by vanilla's
     * own {@link net.minecraft.world.item.ItemCooldowns} rather than anything bespoke. Applied by
     * {@link com.ascension.combat.internal.weapons.DivineSundererEffects}. Modeled as a
     * {@link SwordItem} for now, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> DIVINE_SUNDERER = ITEMS.register(
            "divine_sunderer",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3.0f, -2.8f))));

    /**
     * Sixth out the door, and the second weapon to deal magic damage after {@link
     * #DIVINE_SUNDERER} (combat-api.md section 6). Alternating hits build toward a burst: every
     * other hit primes it, the hit after that discharges a magic-damage proc (max-HP% of the
     * target, through {@link com.ascension.combat.internal.mechanics.MagicDamage}) and shields
     * the wielder, gated by its own cooldown rather than the alternation itself &mdash; so
     * missing the cooldown window still consumes the alternation instead of stalling it. Applied
     * by {@link com.ascension.combat.internal.weapons.DarkenBladeEffects}. Modeled as a
     * {@link SwordItem} for now, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> DARKEN_BLADE = ITEMS.register(
            "darken_blade",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3.0f, -2.6f))));

    /**
     * Seventh out the door, and the third weapon to deal magic damage after
     * {@link #DIVINE_SUNDERER} and {@link #DARKEN_BLADE} (combat-api.md section 6). Revised by
     * Sanchit 2026-09-08 to match Guinsoo's Rageblade's real identity: every hit adds uncapped
     * (shadow-limited) attack speed that ramps down over several seconds of inactivity rather
     * than bursting and resetting, and every hit also deals flat on-hit magic damage &mdash;
     * doubled every third hit, so it reads as landing two hits at once. Applied by
     * {@link com.ascension.combat.internal.weapons.RagefangEffects}. Modeled as a
     * {@link SwordItem} for now, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> RAGEFANG = ITEMS.register(
            "ragefang",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 2.0f, -2.0f))));

    /**
     * Eighth out the door (combat-api.md section 6), and `ORIGINAL` &mdash; no direct League
     * analog. Two mechanics instead of a passive proc: attack speed derived live from the
     * wielder's current Attack Damage rather than the weapon carrying its own fixed rating
     * (recomputed every player tick, so it stays correct as other AD sources come and go), and
     * omnivamp &mdash; heal for a percentage of any damage dealt, not just physical &mdash; that
     * grows the lower the wielder's own HP falls. Applied by
     * {@link com.ascension.combat.internal.weapons.EndlessHungerEffects}. Base attack speed left
     * near vanilla's default rather than the usual sword penalty, since the dynamic bonus is
     * meant to replace a flat rating, not stack on top of a handicap. Modeled as a
     * {@link SwordItem} for now, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> ENDLESS_HUNGER = ITEMS.register(
            "endless_hunger",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 6.0f, 0.0f))));

    /**
     * Ninth out the door (combat-api.md section 6). Passive: flat armor penetration (lethality) on
     * every hit, always on, no attachment needed &mdash; unlike {@link #DARIUS_AXE}'s compounding
     * shred, this doesn't build up or decay. Active: right-click to blink forward a short
     * distance, then gain a move-speed boost for a few seconds, on a cooldown &mdash; replaces the
     * catalog's original phasing-through-mobs identity, which turned out to need a mixin banned in
     * Tier 1 by ADR-0003 rule 2 (see
     * {@link com.ascension.combat.internal.weapons.WraithbladeEffects}'s javadoc). Sanchit's call,
     * 2026-09-08. Modeled as a {@link SwordItem} for now, same placeholder reasoning as
     * {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> WRAITHBLADE = ITEMS.register(
            "wraithblade",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3.0f, -2.4f))));

    /**
     * Tenth out the door, and the second ranged weapon after {@link #MORTAL_REMINDER}
     * (combat-api.md section 6). On-hit: small magic damage plus an uncapped stack of corruption
     * on the target, refreshed as long as it keeps getting hit within the catalog's own 5s window.
     * Active: draw for 3x the normal time ({@code BowItem.MAX_DRAW_DURATION}, 20 ticks &mdash; so
     * 60) and the shot that lands consumes every corruption stack on its target as magic damage
     * equal to that target's missing HP. A plain {@link BowItem} for the same reason
     * {@link #MORTAL_REMINDER} is: the identity is entirely in the on-hit proc and the draw-time
     * read, not the shot itself. Applied by
     * {@link com.ascension.combat.internal.weapons.HeartwoodOfPallasEffects}, which reads {@code
     * ArrowLooseEvent}'s charge value directly rather than reimplementing vanilla's draw-power
     * curve. True multi-target piercing (the catalog's "pierces") isn't implemented &mdash; vanilla's
     * pierce level on a fired arrow is set internally from the bow's own Piercing enchantment, with
     * no public setter reachable without a mixin, so this ships as a single-target execute for now.
     * No art yet, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> HEARTWOOD_OF_PALLAS = ITEMS.register(
            "heartwood_of_pallas",
            () -> new BowItem(new Item.Properties().durability(384)));

    /**
     * Eleventh out the door, and the last ranged weapon (combat-api.md section 6). Revised by
     * Sanchit 2026-09-08: sneak + right-click dashes the wielder forward (real, collidable
     * movement, not Wraithblade's teleport) and fires a homing shot at whoever they most recently
     * damaged, or the lowest-HP nearby target if nothing recent, dealing bonus physical damage
     * scaling with the target's missing HP, on a cooldown. Plain right-click is untouched vanilla
     * bow behaviour &mdash; see
     * {@link com.ascension.combat.internal.weapons.GaleforceEffects}'s javadoc for why the active
     * needs sneak to disambiguate from a normal draw. A plain {@link BowItem}, same reasoning as
     * {@link #MORTAL_REMINDER}. No art yet, same placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> GALEFORCE = ITEMS.register(
            "galeforce",
            () -> new BowItem(new Item.Properties().durability(384)));

    /**
     * Twelfth out the door, the first armor piece, and one of two Helmets (combat-api.md section
     * 6). Gains a resistance stack (bonus armor and magic resist) every second in combat, up to a
     * cap; full stacks double the bonus, and the moment of reaching them carries a small chance to
     * backfire for a chunk of the wearer's own max HP. Needs {@code CombatStateRegistry} (section
     * 4), the first item in the catalog that does &mdash; see
     * {@link com.ascension.combat.internal.mechanics.CombatStateTracking}, stood up alongside this
     * item since it's the first real consumer. Applied by
     * {@link com.ascension.combat.internal.armor.ProteanMaskEffects}. Its material is {@link
     * ArmorMaterials#DIAMOND} in every number &mdash; same placeholder reasoning every weapon in
     * this catalog uses {@link Tiers#DIAMOND} for &mdash; but its own on the body: see {@link
     * #armor}.
     */
    public static final Supplier<Item> PROTEAN_MASK = ITEMS.register(
            "protean_mask",
            armor("protean_mask", ArmorItem.Type.HELMET));

    /**
     * Thirteenth out the door, and the second and last Helmet (combat-api.md section 6).
     * Regenerates a shield after a few seconds out of combat (reusing {@code
     * CombatStateRegistry}, same as {@link #PROTEAN_MASK}); absorbing a hit with it triggers a
     * burst of move speed and lethality. Composes entirely out of mechanics already built for
     * other items &mdash; vanilla absorption for the shield (same as {@link #DARKEN_BLADE}), the
     * flat-armor-penetration reduction modifier for lethality (same as {@link #WRAITHBLADE}), and
     * {@link com.ascension.combat.internal.mechanics.DurationState} for the burst window (same as
     * {@link #WRAITHBLADE}'s speed boost) &mdash; no new infrastructure needed. Applied by
     * {@link com.ascension.combat.internal.armor.EdgeOfNightEffects}. Same placeholder material
     * reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> EDGE_OF_NIGHT = ITEMS.register(
            "edge_of_night",
            armor("edge_of_night", ArmorItem.Type.HELMET));

    /**
     * Fourteenth out the door, and the first of two Chestplates (combat-api.md section 6). Flat
     * armor and magic resist while worn; the active grants a shield scaling with the number of
     * nearby living entities, on a cooldown. The first item to use the shared
     * {@link com.ascension.combat.internal.net.ActivateArmorAbilityPayload} key rather than a
     * right-click, since it's worn, not held &mdash; see
     * {@link com.ascension.combat.client.CombatKeybinds}'s javadoc for why that needed a new
     * key instead of reusing any existing input. Applied by
     * {@link com.ascension.combat.internal.armor.GargoyleStoneplateEffects}. Same placeholder
     * material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> GARGOYLE_STONEPLATE = ITEMS.register(
            "gargoyle_stoneplate",
            armor("gargoyle_stoneplate", ArmorItem.Type.CHESTPLATE));

    /**
     * Fifteenth out the door, and the second and last Chestplate (combat-api.md section 6). On
     * death, while off cooldown: stasis (section 5) instead of dying, then revive at half HP. The
     * first item to need Stasis &mdash; invulnerable, immobile, camera forced to third person, see
     * {@link com.ascension.combat.internal.armor.GuardianWingEffects} for exactly what that means
     * in code and where it's a deliberate simplification of the catalog's "untargetable." Same
     * placeholder material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> GUARDIAN_WING = ITEMS.register(
            "guardian_wing",
            armor("guardian_wing", ArmorItem.Type.CHESTPLATE));

    /**
     * Sixteenth out the door, and the first of three Leggings (combat-api.md section 6). Active
     * (the shared armor-ability key, same as {@link #GARGOYLE_STONEPLATE}): a real, collidable
     * dash ({@link com.ascension.combat.internal.mechanics.Dash}, same as {@link #GALEFORCE}),
     * then an area burst of physical damage a short delay later, roughly where the dash ends.
     * Applied by {@link com.ascension.combat.internal.armor.RocketbeltEffects}. Same placeholder
     * material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> ROCKETBELT = ITEMS.register(
            "rocketbelt",
            armor("rocketbelt", ArmorItem.Type.LEGGINGS));

    /**
     * Seventeenth out the door, and the second of three Leggings (combat-api.md section 6,
     * inspired by The Collector). Damage that would leave a target below 5% of its max HP
     * executes outright instead; a kill this way counts as one Looting level higher than the
     * weapon that landed it, translated to this version's real loot mechanics rather than skipped
     * &mdash; see {@link com.ascension.combat.internal.armor.CollectorsPantsEffects}'s javadoc for
     * what "one Looting level" actually means in 1.21.1's loot resolution. Same placeholder
     * material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> COLLECTORS_PANTS = ITEMS.register(
            "collectors_pants",
            armor("collectors_pants", ArmorItem.Type.LEGGINGS));

    /**
     * Eighteenth out the door, and the third and last Legging (combat-api.md section 6),
     * {@code ORIGINAL}. After 10s in the same fight (section 4), pulses magic damage in a radius
     * around the wearer and heals them for the same amount, repeating as long as the fight
     * continues. Reads {@code CombatStateRegistry} the way {@link #PROTEAN_MASK} does, but tracks
     * continuous time in combat rather than a simple in/out check &mdash; see {@link
     * com.ascension.combat.internal.armor.UnendingDespairState} for why that needed its own
     * shape. Applied by {@link com.ascension.combat.internal.armor.UnendingDespairEffects}. Same
     * placeholder material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> UNENDING_DESPAIR = ITEMS.register(
            "unending_despair",
            armor("unending_despair", ArmorItem.Type.LEGGINGS));

    /**
     * Nineteenth out the door, and the first of six Boots (combat-api.md section 6): flat magic
     * resist while worn. Applied by {@link com.ascension.combat.internal.armor.BootStats} as a
     * {@link com.ascension.combat.internal.armor.StatBoots} rule, same as the next three. Same
     * placeholder material reasoning as {@link #PROTEAN_MASK}.
     */
    public static final Supplier<Item> MERCURY_TREADS = ITEMS.register(
            "mercury_treads",
            armor("mercury_treads", ArmorItem.Type.BOOTS));

    /** Twentieth out the door. Flat armor while worn. */
    public static final Supplier<Item> NINJA_TABI = ITEMS.register(
            "ninja_tabi",
            armor("ninja_tabi", ArmorItem.Type.BOOTS));

    /** Twenty-first out the door (Boots of Swiftness). Move speed while worn. */
    public static final Supplier<Item> SWIFTY = ITEMS.register(
            "swifty",
            armor("swifty", ArmorItem.Type.BOOTS));

    /** Twenty-second out the door. Attack speed while worn. */
    public static final Supplier<Item> BERSERKERS_GREAVES = ITEMS.register(
            "berserkers_greaves",
            armor("berserkers_greaves", ArmorItem.Type.BOOTS));

    /**
     * Twenty-third out the door, {@code ORIGINAL}. Armor and magic penetration while worn.
     * Originally split across two files (an armor-only reduction modifier plus a
     * {@code MagicDamage}-specific special case); since 2026-09-20 it's one plain {@link
     * com.ascension.combat.internal.armor.BootStats} contribution to {@link
     * com.ascension.combat.internal.CombatAttributes#PENETRATION_PERCENT}, a single stat now read
     * centrally by both {@link com.ascension.combat.internal.mechanics.PhysicalDamage} and {@link
     * com.ascension.combat.internal.mechanics.MagicDamage}.
     */
    public static final Supplier<Item> WIZARDS_AGONY = ITEMS.register(
            "wizards_agony",
            armor("wizards_agony", ArmorItem.Type.BOOTS));

    /**
     * Twenty-fourth out the door, and the sixth and last Boots (combat-api.md section 6),
     * {@code ORIGINAL}. Omnivamp while worn, same shape as {@link #ENDLESS_HUNGER}'s own but a
     * flat contribution rather than a dynamic one. A plain {@link
     * com.ascension.combat.internal.armor.BootStats} rule since 2026-09-20, once {@link
     * com.ascension.combat.internal.CombatAttributes#OMNIVAMP} became a real attribute.
     */
    public static final Supplier<Item> TASTED_BLOOD = ITEMS.register(
            "tasted_blood",
            armor("tasted_blood", ArmorItem.Type.BOOTS));

    /**
     * Twenty-fifth out the door, and the first of two Off-hand items (combat-api.md section 6),
     * the catalog's last slot. A plain {@link Item}, not an {@link ArmorItem} &mdash; there is no
     * {@code ArmorItem.Type} for the off-hand slot, since vanilla doesn't treat it as armor (a
     * shield isn't either). Active (the shared armor-ability key, same as {@link
     * #GARGOYLE_STONEPLATE} and {@link #ROCKETBELT}): stasis for a short duration, on a cooldown.
     * The third real consumer of that key, and the first not worn in a body-armor slot, confirming
     * it was always "whatever's equipped with an active," not "armor specifically." Applied by
     * {@link com.ascension.combat.internal.armor.ZoniasHourglassEffects}, reusing {@link
     * com.ascension.combat.internal.mechanics.Stasis} from {@link #GUARDIAN_WING}. No art yet, same
     * placeholder reasoning as {@link #DARIUS_AXE}.
     */
    public static final Supplier<Item> ZONIAS_HOURGLASS = ITEMS.register(
            "zonias_hourglass",
            () -> new Item(new Item.Properties()));

    /**
     * Twenty-sixth out the door, and the last item in the catalog (combat-api.md section 6).
     * Converts a percentage of max HP into bonus attack damage while worn; taking a big hit
     * auto-triggers a shield (vanilla absorption, same as every other Shield/Absorb consumer) plus
     * a temporary lifesteal window, on a cooldown. Applied by
     * {@link com.ascension.combat.internal.armor.SterakHarnessEffects}, whose javadoc explains why
     * the temporary lifesteal is implemented the same shape as this catalog's omnivamp (heals off
     * any damage dealt) rather than physical-only &mdash; nothing in the catalog tags a damage
     * instance's type generically yet. A plain {@link Item}, same reasoning as
     * {@link #ZONIAS_HOURGLASS}.
     */
    public static final Supplier<Item> STERAKS_HARNESS = ITEMS.register(
            "steraks_harness",
            () -> new Item(new Item.Properties()));

    /**
     * Not part of the catalog (section 6) &mdash; a debug-only weapon added 2026-09-19 to verify
     * the new defense-stat curve (section 3, {@link com.ascension.combat.internal.mechanics.DefenseFormula}):
     * every hit deals a flat 10 magic damage and nothing else, so a target's Magic Resist and its
     * reduction can be watched in isolation, without a physical component muddying the number.
     * Applied by {@link com.ascension.combat.internal.weapons.TestMagicBladeEffects}. Deliberately
     * left out of {@link #addToCreativeTabs} &mdash; {@code /give} only, never meant to be found by
     * playing normally.
     */
    public static final Supplier<Item> TEST_MAGIC_BLADE = ITEMS.register(
            "test_magic_blade",
            () -> new SwordItem(Tiers.WOOD, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 0.0f, -2.4f))));

    private CombatContent() {
    }

    /**
     * An armor item with its own material: {@link ArmorMaterials#DIAMOND}'s numbers, copied
     * rather than restated so no stat can drift from where it was, and its own look on the body,
     * {@code textures/models/armor/<id>_layer_1.png} (helmets, chestplates, boots) or
     * {@code _layer_2.png} (leggings), drawn by tools/generate-combat-art.py. One material per
     * item because each is its own League item, not one piece of a set. The material registers
     * now, with the class; the item reads it lazily ({@link ArmorItem} memoizes its modifiers),
     * so registry event order doesn't matter.
     */
    private static Supplier<Item> armor(String id, ArmorItem.Type type) {
        Holder<ArmorMaterial> material = ARMOR_MATERIALS.register(id, () -> {
            ArmorMaterial diamond = ArmorMaterials.DIAMOND.value();
            return new ArmorMaterial(diamond.defense(), diamond.enchantmentValue(), diamond.equipSound(),
                    diamond.repairIngredient(),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, id))),
                    diamond.toughness(), diamond.knockbackResistance());
        });
        return () -> new ArmorItem(material, type, new Item.Properties());
    }

    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(CombatContent::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(VEIGOS_BLADE.get());
            event.accept(DARIUS_AXE.get());
            event.accept(SPEAR_OF_SHOJIN.get());
            event.accept(MORTAL_REMINDER.get());
            event.accept(DIVINE_SUNDERER.get());
            event.accept(DARKEN_BLADE.get());
            event.accept(RAGEFANG.get());
            event.accept(ENDLESS_HUNGER.get());
            event.accept(WRAITHBLADE.get());
            event.accept(HEARTWOOD_OF_PALLAS.get());
            event.accept(GALEFORCE.get());
            event.accept(PROTEAN_MASK.get());
            event.accept(EDGE_OF_NIGHT.get());
            event.accept(GARGOYLE_STONEPLATE.get());
            event.accept(GUARDIAN_WING.get());
            event.accept(ROCKETBELT.get());
            event.accept(COLLECTORS_PANTS.get());
            event.accept(UNENDING_DESPAIR.get());
            event.accept(MERCURY_TREADS.get());
            event.accept(NINJA_TABI.get());
            event.accept(SWIFTY.get());
            event.accept(BERSERKERS_GREAVES.get());
            event.accept(WIZARDS_AGONY.get());
            event.accept(TASTED_BLOOD.get());
            event.accept(ZONIAS_HOURGLASS.get());
            event.accept(STERAKS_HARNESS.get());
        }
    }
}
