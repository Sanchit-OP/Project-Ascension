package com.ascension.combat.internal.dev;

import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.DamageKinds;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.mechanics.MagicHitPulse;
import com.ascension.combat.internal.mobs.DifficultyScaling;
import com.ascension.combat.internal.net.MagicHitPayload;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A development aid, dormant unless a file named {@code damage-checks.txt} sits in the game directory:
 * on a dedicated server it plays the physical-or-magic rule (docs/technical/damage-kinds.md) through
 * the real damage event, prints {@code PASS} or {@code FAIL} lines tagged {@code [damage-checks]}, and
 * stops the server. No client and no player are involved: the target is a pig given a known Armor and
 * Magic Resist, hurt directly with the damage sources the game's own mobs use.
 *
 * <p>The target starts with Magic Resist 100 and Armor 0 (a hit that gets through the Magic Resist is
 * halved) and then the other way round (Armor 100, Magic Resist 0). So for each kind of attack the
 * numbers say which defence it met: a blast halved by Magic Resist and not by Armor is a magic hit.
 */
public final class DamageChecks {

    private static final String TRIGGER = "damage-checks.txt";
    private static final float HIT = 20.0f;
    private static final float TOLERANCE = 0.02f;

    private static MinecraftServer server;
    private static int settle;
    private static boolean armed;
    private static boolean finished;
    private static int checks;
    private static int failures;
    private static FakeConnection connection;
    private static ServerPlayer standIn;

    private DamageChecks() {
    }

    public static void register(IEventBus gameBus) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(TRIGGER);
        if (!Files.exists(trigger)) {
            return;
        }
        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return;
        }
        armed = true;
        gameBus.addListener((ServerStartedEvent event) -> server = event.getServer());
        gameBus.addListener(DamageChecks::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed || finished || server == null) {
            return;
        }
        settle++;
        // Part one once the world has settled; part two eighty ticks later, when the stand-in player has finished
        // joining and its sixty ticks of join protection are over (a hit before that does nothing at all).
        if (settle == 200) {
            guarded(() -> run(server.overworld()));
        } else if (settle == 280) {
            guarded(() -> runPlayer(server.overworld()));
        } else if (settle == 300) {
            guarded(DamageChecks::checkEndlessHungerCeiling);
        } else if (settle == 310) {
            guarded(DamageChecks::checkEnchantable);
        } else if (settle == 320) {
            guarded(DamageChecks::checkEndlessHungerBase);
        } else if (settle == 330) {
            guarded(() -> checkEnchantDamage(server.overworld()));
        } else if (settle == 340) {
            guarded(() -> checkLootingAndSweeping(server.overworld()));
        } else if (settle == 350) {
            guarded(() -> checkLegendaryWeapons(server.overworld()));
        } else if (settle == 360) {
            guarded(DamageChecks::checkGrowthNodes);
            finished = true;
            log("RESULT: " + checks + " checks, " + failures + " failures");
            server.halt(false);
        }
    }

    private static void guarded(Runnable part) {
        try {
            part.run();
        } catch (RuntimeException e) {
            fail("the checks threw " + e);
            e.printStackTrace();
        }
    }

    private static void run(ServerLevel level) {
        Zombie zombie = EntityType.ZOMBIE.create(level);
        Pig pig = EntityType.PIG.create(level);
        if (zombie == null || pig == null) {
            fail("could not make the stand-in mobs");
            return;
        }
        pig.moveTo(level.getSharedSpawnPos().getX() + 0.5, level.getSharedSpawnPos().getY() + 1.0,
                level.getSharedSpawnPos().getZ() + 0.5);
        level.addFreshEntity(pig);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);

        var damage = level.damageSources();
        Entity warden = EntityType.WARDEN.create(level);
        WitherSkull witherSkull = EntityType.WITHER_SKULL.create(level);
        Arrow arrow = EntityType.ARROW.create(level);

        checkBossHealth(level);

        log("--- the tags");
        var damageTypes = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (ResourceKey<DamageType> key : List.of(
                DamageTypes.EXPLOSION, DamageTypes.SONIC_BOOM, DamageTypes.LAVA, DamageTypes.ON_FIRE, DamageTypes.LIGHTNING_BOLT,
                DamageTypes.MAGIC, DamageTypes.INDIRECT_MAGIC, DamageTypes.WITHER, DamageTypes.WITHER_SKULL, DamageTypes.DRAGON_BREATH)) {
            Holder<DamageType> type = damageTypes.getHolderOrThrow(key);
            expect(key.location() + " is magic", type.is(DamageKinds.MAGIC), true);
        }
        for (ResourceKey<DamageType> key : List.of(
                DamageTypes.MOB_ATTACK, DamageTypes.PLAYER_ATTACK, DamageTypes.ARROW, DamageTypes.MOB_PROJECTILE, DamageTypes.THORNS,
                DamageTypes.FALL, DamageTypes.DROWN, DamageTypes.STARVE, DamageTypes.GENERIC, DamageTypes.CACTUS)) {
            Holder<DamageType> type = damageTypes.getHolderOrThrow(key);
            expect(key.location() + " is not magic", type.is(DamageKinds.MAGIC), false);
        }

        // ---- Magic Resist 100, Armor 0: a magic hit is halved, a physical one is not -----------------
        log("--- Magic Resist 100, Armor 0");
        setStat(pig, CombatAttributes.MAGIC_RESIST, 100.0);
        setStat(pig, CombatAttributes.ARMOR, 0.0);
        hit("a zombie's fist (physical)", pig, damage.mobAttack(zombie), HIT);
        if (warden != null) {
            hit("the Warden's fist (physical)", pig, damage.mobAttack((LivingEntity) warden), HIT);
            hit("the Warden's sonic boom (magic)", pig, damage.sonicBoom(warden), HIT / 2);
        }
        hit("a Creeper's blast (magic)", pig, damage.explosion(null, zombie), HIT / 2);
        hit("burning (magic)", pig, damage.onFire(), HIT / 2);
        hit("lava (magic)", pig, damage.lava(), HIT / 2);
        hit("standing in fire (magic)", pig, damage.inFire(), HIT / 2);
        hit("lightning (magic)", pig, damage.lightningBolt(), HIT / 2);
        hit("poison or a potion (magic)", pig, damage.magic(), HIT / 2);
        hit("a witch's potion (magic)", pig, damage.indirectMagic(zombie, zombie), HIT / 2);
        hit("the Wither's decay (magic)", pig, damage.wither(), HIT / 2);
        hit("the dragon's breath (magic)", pig, damage.source(DamageTypes.DRAGON_BREATH, zombie), HIT / 2);
        if (witherSkull != null) {
            hit("a Wither skull (magic)", pig, damage.witherSkull(witherSkull, zombie), HIT / 2);
        }
        if (arrow != null) {
            // Every arrow carries its shooter's Attack Damage on top (PhysicalDamage.applyArrowAttackDamage).
            hit("a skeleton's arrow (physical, plus the shooter's Attack Damage)", pig, damage.arrow(arrow, zombie),
                    HIT + (float) zombie.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        hit("a fall (neither)", pig, damage.fall(), HIT);
        hit("starving (neither)", pig, damage.starve(), HIT);

        // ---- The other way round: Armor 100, Magic Resist 0 ------------------------------------------
        log("--- Armor 100, Magic Resist 0");
        setStat(pig, CombatAttributes.MAGIC_RESIST, 0.0);
        setStat(pig, CombatAttributes.ARMOR, 100.0);
        hit("a zombie's fist (physical) is halved by Armor", pig, damage.mobAttack(zombie), HIT / 2);
        hit("a Creeper's blast (magic) ignores Armor", pig, damage.explosion(null, zombie), HIT);
        hit("lava (magic) ignores Armor", pig, damage.lava(), HIT);
        hit("the sonic boom (magic) ignores Armor", pig, damage.sonicBoom(zombie), HIT);

        // ---- Our own magic hits are cut once, not twice ----------------------------------------------
        log("--- our own magic hits");
        setStat(pig, CombatAttributes.MAGIC_RESIST, 100.0);
        setStat(pig, CombatAttributes.ARMOR, 0.0);
        pig.setHealth(pig.getMaxHealth());
        float before = pig.getHealth();
        pig.invulnerableTime = 0;
        MagicDamage.applyStandalone(pig, damage.magic(), HIT);
        expectNear("a rune's or an item's magic strike is cut by Magic Resist once, not twice", before - pig.getHealth(), HIT / 2);
        pig.setHealth(pig.getMaxHealth());
        before = pig.getHealth();
        pig.invulnerableTime = 0;
        MagicDamage.hurtSettled(pig, damage.magic(), HIT);
        expectNear("a cost the wearer pays (Protean Mask's backfire) is not softened", before - pig.getHealth(), HIT);

        // ---- Magic Penetration cuts the resist --------------------------------------------------------
        log("--- Magic Penetration");
        zombie.getAttribute(CombatAttributes.MAGIC_PENETRATION_PERCENT).setBaseValue(0.5);
        hit("a blast from a 50% Magic Penetration attacker meets Magic Resist 50", pig, damage.explosion(null, zombie),
                (float) (HIT * Math.pow(0.5, 0.5)));
        zombie.getAttribute(CombatAttributes.MAGIC_PENETRATION_PERCENT).setBaseValue(0.0);

        // ---- Cataclysm, when it is in the game --------------------------------------------------------
        log("--- Cataclysm");
        EntityType<?> fireball = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("cataclysm", "ignis_fireball"));
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("cataclysm", "ignis_fireball"))) {
            log("SKIP Cataclysm is not loaded");
        } else {
            Entity ignisFireball = fireball.create(level);
            expect("Ignis' fireball is in the magic-attack list", ignisFireball != null && ignisFireball.getType().is(DamageKinds.MAGIC_ATTACK), true);
            if (ignisFireball != null) {
                hit("Ignis' fireball (generic projectile type, magic by its entity)", pig, damage.mobProjectile(ignisFireball, zombie), HIT / 2);
            }
            if (arrow != null) {
                hit("the same projectile type from an arrow stays physical", pig, damage.mobProjectile(arrow, zombie),
                        HIT + (float) zombie.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
            var flameStrike = ResourceLocation.fromNamespaceAndPath("cataclysm", "flame_strike");
            damageTypes.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, flameStrike))
                    .ifPresentOrElse(
                            type -> expect("Cataclysm's flame strike type is magic", type.is(DamageKinds.MAGIC), true),
                            () -> fail("Cataclysm's flame_strike damage type is missing"));
            var golem = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("cataclysm", "ender_golem")).create(level);
            if (golem instanceof LivingEntity livingGolem) {
                hit("the Ender Golem's fist (physical)", pig, damage.mobAttack(livingGolem), HIT);
            }
        }

        pig.discard();

        // The stand-in player for part two: joins now, is hurt eighty ticks later.
        UUID id = UUID.nameUUIDFromBytes("OfflinePlayer:DamageChecks".getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, "DamageChecks");
        connection = new FakeConnection();
        standIn = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        server.getPlayerList().placeNewPlayer(connection, standIn, CommonListenerCookie.createInitial(profile, false));
        // The test worlds default to creative, where nobody can be hurt.
        standIn.setGameMode(GameType.SURVIVAL);
    }

    /** Part two: the screen tint reaches a real player on a real (stand-in) connection, for magic hits only. */
    private static void runPlayer(ServerLevel level) {
        log("--- the screen tint");
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (standIn == null || zombie == null) {
            fail("no stand-in player");
            return;
        }
        var damage = level.damageSources();

        connection.forget();
        expect("a blast landed on the player", hurtPlayer(damage.explosion(null, zombie), 4.0f) > 0.0f, true);
        var blast = connection.sent(MagicHitPayload.class);
        expectCount("a blast tells the player once", blast.size(), 1);
        if (!blast.isEmpty()) {
            float strength = blast.get(0).strength();
            expect("and the pulse is between the weakest and the strongest (" + strength + ")",
                    strength >= MagicHitPulse.MIN_STRENGTH && strength <= 1.0f, true);
        }

        connection.forget();
        expect("a zombie's fist landed on the player", hurtPlayer(damage.mobAttack(zombie), 4.0f) > 0.0f, true);
        expectCount("a zombie's fist tells the player nothing", connection.sent(MagicHitPayload.class).size(), 0);

        connection.forget();
        expect("a fall landed on the player", hurtPlayer(damage.fall(), 4.0f) > 0.0f, true);
        expectCount("a fall tells the player nothing", connection.sent(MagicHitPayload.class).size(), 0);

        connection.forget();
        expect("lava landed on the player", hurtPlayer(damage.lava(), 4.0f) > 0.0f, true);
        expectCount("lava tells the player once", connection.sent(MagicHitPayload.class).size(), 1);

        connection.forget();
        standIn.getAttribute(CombatAttributes.MAGIC_RESIST).setBaseValue(300.0);
        float taken = hurtPlayer(damage.explosion(null, zombie), 0.0f);
        expect("a hit of no damage did nothing", taken <= 0.0f, true);
        expectCount("and tells the player nothing", connection.sent(MagicHitPayload.class).size(), 0);
        standIn.getAttribute(CombatAttributes.MAGIC_RESIST).setBaseValue(0.0);

        // Part three: Endless Hunger's swing speed, a few ticks later so its per-tick effect has run.
        standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CombatContent.ENDLESS_HUNGER.get()));
        standIn.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_attack_damage"),
                500.0, AttributeModifier.Operation.ADD_VALUE));
    }

    /** With 500 more Attack Damage than any build has, Endless Hunger still may not swing faster than its ceiling. */
    private static void checkEndlessHungerCeiling() {
        log("--- Endless Hunger's swing speed");
        tickStandIn();
        double speed = standIn.getAttributeValue(Attributes.ATTACK_SPEED);
        expect("Endless Hunger with 500 extra Attack Damage swings at its ceiling of 3 a second, no faster (got "
                + String.format("%.2f", speed) + ")", Math.abs(speed - 3.0) < 0.01, true);
        standIn.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_attack_damage"));
    }

    /**
     * Every one of the 26 combat items says whether the game lets it take an enchantment, by role: a sword takes the
     * weapon set (Sharpness, Keen Edge), a bow Power, armour Protection, boots Feather Falling, and all of them Mending and
     * Soulbound; the two off-hand relics are not damageable items, so they take nothing (vanilla enchants only damageable
     * items). Found missing on 2026-10-22: none of our items was in vanilla's enchantable tags.
     */
    private static void checkEnchantable() {
        var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var legendary = BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "legendary")));
        if (legendary.isEmpty()) {
            fail("the legendary item tag is missing");
            return;
        }
        int seen = 0;
        for (Holder<net.minecraft.world.item.Item> holder : legendary.get()) {
            seen++;
            ItemStack stack = new ItemStack(holder);
            String id = holder.unwrapKey().orElseThrow().location().getPath();
            var item = stack.getItem();
            boolean sword = item instanceof net.minecraft.world.item.SwordItem;
            boolean bow = item instanceof net.minecraft.world.item.BowItem;
            boolean armour = item instanceof net.minecraft.world.item.ArmorItem;
            boolean boots = armour && ((net.minecraft.world.item.ArmorItem) item).getType()
                    == net.minecraft.world.item.ArmorItem.Type.BOOTS;
            boolean relic = !sword && !bow && !armour;
            expect(id + " is enchantable", stack.isEnchantable(), !relic);
            if (relic) {
                continue;
            }
            expect(id + " takes Mending", supports(enchantments, stack, "minecraft", "mending"), true);
            expect(id + " takes Unbreaking", supports(enchantments, stack, "minecraft", "unbreaking"), true);
            expect(id + " takes Soulbound", supports(enchantments, stack, "ascension_combat", "soulbound"), true);
            if (sword) {
                expect(id + " takes Sharpness", supports(enchantments, stack, "minecraft", "sharpness"), true);
                expect(id + " takes Keen Edge", supports(enchantments, stack, "ascension_combat", "keen_edge"), true);
                expect(id + " takes Leech", supports(enchantments, stack, "ascension_combat", "leech"), true);
            } else if (bow) {
                expect(id + " takes Power", supports(enchantments, stack, "minecraft", "power"), true);
                expect(id + " refuses Sharpness", supports(enchantments, stack, "minecraft", "sharpness"), false);
            } else {
                expect(id + " takes Protection", supports(enchantments, stack, "minecraft", "protection"), true);
                expect(id + " refuses Sharpness", supports(enchantments, stack, "minecraft", "sharpness"), false);
                expect(id + (boots ? " takes" : " refuses") + " Feather Falling",
                        supports(enchantments, stack, "minecraft", "feather_falling"), boots);
            }
        }
        expectCount("legendary items seen", seen, 26);
    }

    /**
     * What the enchantments are worth in a real swing (2026-10-22, after the buff): the stand-in player hits a zombie, which is undead
     * and has no armour, with an iron sword plain, with Sharpness V and with Smite V, and the extra damage is read off the zombie's health.
     * Sharpness V is +5 Attack Damage, Smite V the vanilla +12.5 against the undead, and the Vitality a piece gives is read off the player.
     */
    private static void checkEnchantDamage(ServerLevel level) {
        log("--- enchantments in a real swing");
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (standIn == null || zombie == null) {
            fail("no stand-in player or zombie");
            return;
        }
        zombie.moveTo(standIn.getX() + 1.0, standIn.getY(), standIn.getZ());
        level.addFreshEntity(zombie);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(CombatAttributes.ARMOR).setBaseValue(0.0);
        // A huge attack speed, so every swing is at full strength whatever the cooldown.
        standIn.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing"), 1000.0,
                AttributeModifier.Operation.ADD_VALUE));

        float plain = swing(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD), zombie);
        ItemStack sharp = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        sharp.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 5);
        float sharpDamage = swing(sharp, zombie);
        ItemStack smite = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        smite.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE), 5);
        float smiteDamage = swing(smite, zombie);

        // The zombie carries some Armor of its difficulty tier, which cuts every point the same way, so the bonus is checked after it.
        float through = (float) com.ascension.combat.internal.mechanics.DefenseFormula.multiplier(
                zombie.getAttributeValue(CombatAttributes.ARMOR));
        log("plain " + plain + ", Sharpness V " + sharpDamage + ", Smite V " + smiteDamage + ", the zombie lets "
                + String.format("%.4f", through) + " through");
        expect("a plain swing does damage (got " + plain + ")", plain > 0.0f, true);
        expectNear("Sharpness V adds 7.5 damage to a swing, after the zombie's Armor", sharpDamage - plain, 7.5f * through);
        // Smite V is +5 Attack Damage against anything (one point a level) and vanilla's +12.5 on top against the undead: 17.5 in all.
        expectNear("Smite V adds 17.5 damage to a swing at the undead (5 Attack Damage and vanilla's 12.5), after its Armor",
                smiteDamage - plain, 17.5f * through);
        // The 5 Attack Damage is a real attribute, so it reaches the abilities that scale off it: Veigo's Blade takes a share of the
        // target's current health for each point, so the same Veigo with Smite V must beat the plain one by more than the flat part.
        ItemStack veigo = new ItemStack(CombatContent.VEIGOS_BLADE.get());
        float veigoPlain = swing(veigo, zombie);
        ItemStack veigoSmite = new ItemStack(CombatContent.VEIGOS_BLADE.get());
        veigoSmite.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE), 5);
        float veigoSmiteDamage = swing(veigoSmite, zombie);
        log("Veigo's Blade plain " + veigoPlain + ", with Smite V " + veigoSmiteDamage);
        expect("Smite V makes Veigo's Blade hit for more than the flat 12.5 (its Attack Damage scaling saw the bonus)",
                veigoSmiteDamage - veigoPlain > 17.5f * through + 50.0f, true);
        // A zombie is not an arthropod: Bane of Arthropods adds only its 5 Attack Damage against it, not vanilla's 12.5.
        ItemStack bane = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        bane.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS), 5);
        expectNear("Bane of Arthropods adds only its 5 Attack Damage against a zombie", swing(bane, zombie) - plain, 5.0f * through);

        standIn.getAttribute(Attributes.ATTACK_SPEED).removeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing"));
        zombie.discard();

        // Vitality III on a piece of armour: +12 Max Health over the same helmet plain.
        standIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(CombatContent.PROTEAN_MASK.get()));
        tickStandIn();
        double healthBefore = standIn.getMaxHealth();
        ItemStack helmet = new ItemStack(CombatContent.PROTEAN_MASK.get());
        helmet.enchant(enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "vitality"))), 3);
        standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
        tickStandIn();
        double with = standIn.getMaxHealth();
        float itemHealth = (float) (with - healthBefore);
        log("Max Health " + healthBefore + " -> " + with + " with a Vitality III helmet over the same helmet plain");
        expectNear("Vitality III on a piece adds 15 Max Health", itemHealth, 15.0f);
        standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);

        // The mace and the trident were vanilla's 6 at 0.6 and 9 at 1.1, which took 787 s and 286 s to kill the standard boss (a netherite sword: 222 s).
        standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.MACE));
        tickStandIn();
        expectNear("the mace hits for 12 (its smash from a fall is on top)", (float) standIn.getAttributeValue(Attributes.ATTACK_DAMAGE), 12.0f);
        expectNear("the mace swings at 0.8 a second", (float) standIn.getAttributeValue(Attributes.ATTACK_SPEED), 0.8f);
        standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.TRIDENT));
        tickStandIn();
        expectNear("the trident hits for 15", (float) standIn.getAttributeValue(Attributes.ATTACK_DAMAGE), 15.0f);
        expectNear("the trident swings at 1.5 a second", (float) standIn.getAttributeValue(Attributes.ATTACK_SPEED), 1.5f);
        standIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /**
     * Collector's Pants add one Looting level while their wearer's kill rolls its loot, and Sweeping Edge adds magic damage to the extra targets of
     * a swing (2026-10-22). Looting is read by asking the game what level the killer's weapon has, from a listener that runs after the pants'
     * own, at the moment of the kill; Sweeping Edge by hitting two zombies in the same tick, the second as a sweep's extra target.
     */
    private static void checkLootingAndSweeping(ServerLevel level) {
        log("--- Collector's Pants and Sweeping Edge");
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var looting = enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        standIn.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing2"), 1000.0,
                AttributeModifier.Operation.ADD_VALUE));

        // Looting: a Looting III sword, with and without the pants, read inside the kill.
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        sword.enchant(looting, 3);
        standIn.setItemInHand(InteractionHand.MAIN_HAND, sword);
        int[] inside = {-1};
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> reader =
                event -> inside[0] = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting, standIn);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, reader);
        try {
            standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
            Zombie plainKill = EntityType.ZOMBIE.create(level);
            plainKill.moveTo(standIn.getX() + 1.0, standIn.getY(), standIn.getZ());
            level.addFreshEntity(plainKill);
            plainKill.hurt(level.damageSources().playerAttack(standIn), 100000.0f);
            expectCount("a kill without the pants reads Looting 3", inside[0], 3);

            standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(CombatContent.COLLECTORS_PANTS.get()));
            inside[0] = -1;
            Zombie pantsKill = EntityType.ZOMBIE.create(level);
            pantsKill.moveTo(standIn.getX() + 1.0, standIn.getY(), standIn.getZ());
            level.addFreshEntity(pantsKill);
            pantsKill.hurt(level.damageSources().playerAttack(standIn), 100000.0f);
            expectCount("a kill in the pants reads Looting 4 on a Looting 3 sword", inside[0], 4);
            expectCount("and afterwards the sword is back to Looting 3", net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getEnchantmentLevel(looting, standIn), 3);
            expectCount("and the stack itself never changed", net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(looting, sword), 3);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(reader);
            standIn.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
        }

        // Sweeping Edge: two zombies hit in the same tick, the second well out of vanilla's sweep reach.
        Zombie aimed = EntityType.ZOMBIE.create(level);
        Zombie extra = EntityType.ZOMBIE.create(level);
        aimed.moveTo(standIn.getX() + 1.0, standIn.getY(), standIn.getZ());
        extra.moveTo(standIn.getX() - 6.0, standIn.getY(), standIn.getZ());
        for (Zombie zombie : new Zombie[] {aimed, extra}) {
            level.addFreshEntity(zombie);
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
        }
        ItemStack plainSword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        ItemStack sweepSword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        sweepSword.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE), 3);
        float[] plainHits = sweepPair(plainSword, aimed, extra);
        float[] sweepHits = sweepPair(sweepSword, aimed, extra);
        float ad = com.ascension.combat.internal.mechanics.AbilityScaling.attackDamage(standIn);
        float magic = 3.0f * (com.ascension.combat.internal.enchanting.SweepingEdgeEffects.MAGIC_PER_LEVEL
                + com.ascension.combat.internal.enchanting.SweepingEdgeEffects.MAGIC_PER_LEVEL_PER_AD * ad);
        float through = (float) MagicDamage.passThrough(extra, standIn);
        log("aimed target " + plainHits[0] + " -> " + sweepHits[0] + ", extra target " + plainHits[1] + " -> " + sweepHits[1]
                + " (Sweeping Edge III adds " + magic + " magic, " + through + " of it gets through)");
        expectNear("Sweeping Edge adds nothing to the target the swing was aimed at", sweepHits[0] - plainHits[0], 0.0f);
        expectNear("Sweeping Edge III adds its magic damage to the extra target, after Magic Resist", sweepHits[1] - plainHits[1], magic * through);

        standIn.getAttribute(Attributes.ATTACK_SPEED).removeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing2"));
        standIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        aimed.discard();
        extra.discard();
    }

    /**
     * The eleven legendary weapons carry the raised numbers of 2026-10-22 (Sanchit: the player was far too weak against the bosses), and
     * the two dual-wield pairs add up: the second copy of Ragefang or Endless Hunger counts its damage, and Endless Hunger its Omnivamp and
     * a swing speed that follows the sum. Real swings at a zombie with 1,000 health and no Armor of its own, so a weapon's ability can be
     * worked out by hand: Veigo's Blade takes 4% of the target's current health plus 0.075% per Attack Damage, Ragefang adds 4.5 magic
     * plus 0.15 per Attack Damage.
     */
    private static void checkLegendaryWeapons(ServerLevel level) {
        log("--- the legendary weapons");
        record Expected(String name, net.minecraft.world.item.Item item, float attackDamage) {
        }
        List<Expected> weapons = List.of(
                new Expected("Veigo's Blade", CombatContent.VEIGOS_BLADE.get(), 16.5f),
                new Expected("Darius's Axe", CombatContent.DARIUS_AXE.get(), 34.0f),
                new Expected("Spear of Shojin", CombatContent.SPEAR_OF_SHOJIN.get(), 31.0f),
                new Expected("Divine Sunderer", CombatContent.DIVINE_SUNDERER.get(), 25.5f),
                new Expected("Darken Blade", CombatContent.DARKEN_BLADE.get(), 29.0f),
                new Expected("Ragefang", CombatContent.RAGEFANG.get(), 12.0f),
                new Expected("Endless Hunger", CombatContent.ENDLESS_HUNGER.get(), 24.0f),
                new Expected("Wraithblade", CombatContent.WRAITHBLADE.get(), 32.0f),
                // A bow's Attack Damage is the player's 1 plus its stat.
                new Expected("Mortal Reminder", CombatContent.MORTAL_REMINDER.get(), 24.5f),
                new Expected("Heartwood of Pallas", CombatContent.HEARTWOOD_OF_PALLAS.get(), 22.5f),
                new Expected("Galeforce", CombatContent.GALEFORCE.get(), 23.0f));
        standIn.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        for (Expected weapon : weapons) {
            standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon.item()));
            tickStandIn();
            expectNear(weapon.name() + " carries " + weapon.attackDamage() + " Attack Damage",
                    (float) standIn.getAttributeValue(Attributes.ATTACK_DAMAGE), weapon.attackDamage());
        }
        standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CombatContent.WRAITHBLADE.get()));
        tickStandIn();
        expectNear("Wraithblade carries 40 Lethality", (float) standIn.getAttributeValue(CombatAttributes.LETHALITY), 40.0f);

        // Endless Hunger: one copy 7% Omnivamp; a pair 14%, 2 x 24 - 1 = 47 Attack Damage, and 2.0 a second grown by 0.6% a point.
        standIn.setHealth(standIn.getMaxHealth());
        standIn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CombatContent.ENDLESS_HUNGER.get()));
        tickStandIn();
        expectNear("one Endless Hunger gives 7% Omnivamp", (float) standIn.getAttributeValue(CombatAttributes.OMNIVAMP), 0.07f);
        standIn.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(CombatContent.ENDLESS_HUNGER.get()));
        tickStandIn();
        expectNear("a pair of Endless Hunger gives 14% Omnivamp", (float) standIn.getAttributeValue(CombatAttributes.OMNIVAMP), 0.14f);
        expectNear("a pair of Endless Hunger has 47 Attack Damage", (float) standIn.getAttributeValue(Attributes.ATTACK_DAMAGE), 47.0f);
        expectNear("and swings at 2.56 a second, under its ceiling of 3", (float) standIn.getAttributeValue(Attributes.ATTACK_SPEED), 2.564f);
        standIn.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            fail("could not make the stand-in zombie");
            return;
        }
        zombie.moveTo(standIn.getX() + 1.0, standIn.getY(), standIn.getZ());
        level.addFreshEntity(zombie);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(CombatAttributes.ARMOR).setBaseValue(0.0);
        standIn.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing3"), 1000.0,
                AttributeModifier.Operation.ADD_VALUE));
        float armor = (float) com.ascension.combat.internal.mechanics.DefenseFormula.multiplier(
                zombie.getAttributeValue(CombatAttributes.ARMOR));
        float magic = (float) MagicDamage.passThrough(zombie, standIn);

        float veigo = swing(new ItemStack(CombatContent.VEIGOS_BLADE.get()), zombie);
        expectNear("a swing of Veigo's Blade takes 16.5 plus 4% + 0.075% x 16.5 of 1,000 health, after Armor", veigo,
                (16.5f + 1000.0f * (0.04f + 0.00075f * 16.5f)) * armor);

        // The first hit of a Ragefang is not its third, so its magic is single; the pair swings with the second copy in the off hand.
        float ragefang = swing(new ItemStack(CombatContent.RAGEFANG.get()), zombie);
        expectNear("a swing of Ragefang is 12 plus 4.5 + 0.15 x 12 magic", ragefang, 12.0f * armor + (4.5f + 0.15f * 12.0f) * magic);
        standIn.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(CombatContent.RAGEFANG.get()));
        float pair = swing(new ItemStack(CombatContent.RAGEFANG.get()), zombie);
        expectNear("a swing of a pair of Ragefang is 23 plus 4.5 + 0.15 x 23 magic (both copies count)", pair,
                23.0f * armor + (4.5f + 0.15f * 23.0f) * magic);
        log("Veigo's Blade " + veigo + ", Ragefang " + ragefang + ", a pair of Ragefang " + pair);

        standIn.getAttribute(Attributes.ATTACK_SPEED).removeModifier(
                ResourceLocation.fromNamespaceAndPath("ascension_combat", "damage_checks_swing3"));
        standIn.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        standIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        zombie.discard();
    }

    /**
     * The skill tree's raised numbers (2026-10-22, Sanchit: +0.5 Attack Damage, 1 Lethality and 1 HP were nothing in the bigger picture)
     * reach a real player. The stand-in earns two boss milestones the way the game awards them (3 points each), takes six nodes by the rules
     * (three along the Power side to a notable, three Guard and Fury starts), and its attributes are read before and after: Attack Damage
     * +1 and +2 (the notable Heavy Hand), Lethality +3, Max Health +3, Magic Resist +5, Omnivamp +2%.
     */
    private static void checkGrowthNodes() {
        log("--- the skill tree's numbers on a player");
        var server = standIn.server;
        standIn.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        standIn.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        tickStandIn();
        double health = standIn.getMaxHealth();
        double attackDamage = standIn.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double lethality = standIn.getAttributeValue(CombatAttributes.LETHALITY);
        double magicResist = standIn.getAttributeValue(CombatAttributes.MAGIC_RESIST);
        double omnivamp = standIn.getAttributeValue(CombatAttributes.OMNIVAMP);

        for (String boss : List.of("warden", "wither")) {
            var advancement = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("ascension_combat", "growth/" + boss));
            if (advancement == null) {
                fail("no growth advancement for the " + boss);
                return;
            }
            com.ascension.combat.internal.growth.PlayerGrowth.awardIfMissing(standIn, advancement);
        }
        int points = com.ascension.combat.internal.growth.PlayerGrowth.pointsAvailable(standIn);
        log("the stand-in has " + points + " skill points to spend");
        expect("the two boss milestones leave at least the 6 points the six nodes cost", points >= 6, true);

        List<ResourceLocation> nodes = java.util.stream.Stream.of("r1_nw", "r2_nw", "r3_nw", "r1_se", "r1_s", "r1_e")
                .map(name -> ResourceLocation.fromNamespaceAndPath("ascension_combat", "web/" + name)).toList();
        var failure = com.ascension.combat.internal.growth.PlayerGrowth.takeAll(standIn, nodes);
        expect("the six nodes are all taken by the rules" + (failure == null ? "" : " (stopped at " + failure + ")"), failure == null, true);
        tickStandIn();

        expectNear("Might and Heavy Hand add 3 Attack Damage (1 and 2)",
                (float) (standIn.getAttributeValue(Attributes.ATTACK_DAMAGE) - attackDamage), 3.0f);
        expectNear("Edge adds 3 Lethality", (float) (standIn.getAttributeValue(CombatAttributes.LETHALITY) - lethality), 3.0f);
        expectNear("Vitality adds 3 Max Health", (float) (standIn.getMaxHealth() - health), 3.0f);
        expectNear("Warding adds 5 Magic Resist", (float) (standIn.getAttributeValue(CombatAttributes.MAGIC_RESIST) - magicResist), 5.0f);
        expectNear("Hunger adds 2% Omnivamp", (float) (standIn.getAttributeValue(CombatAttributes.OMNIVAMP) - omnivamp), 0.02f);
        com.ascension.combat.internal.growth.PlayerGrowth.reset(standIn);
        tickStandIn();
        expectNear("a respec gives it all back: Max Health is as it was", (float) standIn.getMaxHealth(), (float) health);
    }

    /** Two attacks by the stand-in in the same tick: the first at {@code aimed}, then one at {@code extra}. Returns what each lost. */
    private static float[] sweepPair(ItemStack weapon, Zombie aimed, Zombie extra) {
        standIn.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        tickStandIn();
        aimed.setHealth(aimed.getMaxHealth());
        extra.setHealth(extra.getMaxHealth());
        aimed.invulnerableTime = 0;
        extra.invulnerableTime = 0;
        float aimedBefore = aimed.getHealth();
        float extraBefore = extra.getHealth();
        standIn.attack(aimed);
        standIn.attack(extra);
        return new float[] {aimedBefore - aimed.getHealth(), extraBefore - extra.getHealth()};
    }

    /** One full-strength swing of {@code weapon} by the stand-in player at {@code target}, and how much health the target lost. */
    private static float swing(ItemStack weapon, LivingEntity target) {
        standIn.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        tickStandIn();
        target.setHealth(target.getMaxHealth());
        target.invulnerableTime = 0;
        float before = target.getHealth();
        standIn.attack(target);
        return before - target.getHealth();
    }

    private static boolean supports(net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> enchantments,
                                    ItemStack stack, String namespace, String path) {
        return enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(namespace, path))).value().isSupportedItem(stack);
    }

    /** At the weapon's own Attack Damage it swings at about 2.3 a second: slow and heavy, not the 4.3 it used to. */
    private static void checkEndlessHungerBase() {
        tickStandIn();
        double speed = standIn.getAttributeValue(Attributes.ATTACK_SPEED);
        double damage = standIn.getAttributeValue(Attributes.ATTACK_DAMAGE);
        expect("Endless Hunger on its own swings at 2 to 2.6 a second (got " + String.format("%.2f", speed)
                + " at " + String.format("%.1f", damage) + " Attack Damage)", speed >= 2.0 && speed <= 2.6, true);
        expect("and hits for an Attack Damage of 24 (got " + String.format("%.1f", damage) + ")", Math.abs(damage - 24.0) < 0.5, true);
    }

    /**
     * Ticks the stand-in player by hand. Its fake connection has no tick, and a player's own tick (gear changes, every
     * per-tick weapon effect) is driven from the connection, so without this nothing per-tick would ever run for it.
     */
    private static void tickStandIn() {
        for (int i = 0; i < 3; i++) {
            standIn.doTick();
        }
    }

    /** Hurts the stand-in player from full health and returns how much health it lost. */
    private static float hurtPlayer(DamageSource source, float amount) {
        standIn.setHealth(standIn.getMaxHealth());
        standIn.invulnerableTime = 0;
        float before = standIn.getHealth();
        standIn.hurt(source, amount);
        return before - standIn.getHealth();
    }

    /**
     * The seven bosses (six, and the Ender Dragon) reach the health the balance gives them. Vanilla clamps Max Health at 1,024 (ADR-0027), so before the
     * ceiling was raised every one of these came out at exactly 1,024. Each is added to the world and read in the same tick
     * (so it never gets to act), then removed.
     */
    private static void checkBossHealth(ServerLevel level) {
        log("--- boss health (vanilla's own ceiling is 1024)");
        expect("Max Health's ceiling is above vanilla's 1024",
                ((RangedAttribute) Attributes.MAX_HEALTH.value()).getMaxValue() > 1024.0, true);
        for (String name : List.of("minecraft:warden", "minecraft:wither", "minecraft:ender_dragon", "cataclysm:ender_golem", "cataclysm:ender_guardian",
                "cataclysm:ignis", "cataclysm:netherite_monstrosity")) {
            ResourceLocation id = ResourceLocation.parse(name);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                log("SKIP " + name + " is not loaded");
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            DifficultyScaling.Scale scale = DifficultyScaling.INSTANCE.scaleFor(type, level.getDifficulty());
            Entity boss = type.create(level);
            if (!scale.hasHealthTotal() || !(boss instanceof LivingEntity living)) {
                fail(name + " has no health total in the data for " + level.getDifficulty());
                continue;
            }
            living.moveTo(level.getSharedSpawnPos().getX() + 0.5, level.getSharedSpawnPos().getY() + 1.0,
                    level.getSharedSpawnPos().getZ() + 0.5);
            level.addFreshEntity(living);
            if (Math.abs(living.getMaxHealth() - scale.healthTotal()) > 0.5) {
                var instance = living.getAttribute(Attributes.MAX_HEALTH);
                log("   " + name + " base " + instance.getBaseValue() + ", modifiers:");
                for (AttributeModifier modifier : instance.getModifiers()) {
                    log("     " + modifier.id() + " " + modifier.operation() + " " + modifier.amount());
                }
            }
            expectNear(name + " has " + (int) scale.healthTotal() + " health on " + level.getDifficulty().getKey()
                    + ", and starts full", living.getMaxHealth(), (float) scale.healthTotal());
            expectNear(name + " starts at full health", living.getHealth(), (float) scale.healthTotal());
            var attackDamage = living.getAttribute(Attributes.ATTACK_DAMAGE);
            log("   " + name + " on " + level.getDifficulty().getKey() + ": health " + living.getMaxHealth() + ", Armor "
                    + living.getAttributeValue(CombatAttributes.ARMOR) + ", Magic Resist " + living.getAttributeValue(CombatAttributes.MAGIC_RESIST)
                    + ", Attack Damage " + (attackDamage == null ? "none (fixed hits)" : String.valueOf(attackDamage.getValue())));
            living.discard();
        }
    }

    // ------------------------------------------------------------------------------------------

    private static void setStat(LivingEntity entity, DeferredHolder<Attribute, Attribute> stat, double value) {
        entity.getAttribute(stat).setBaseValue(value);
    }

    /** Hurts the pig with one hit from full health and checks how much of it landed. */
    private static void hit(String what, Pig pig, DamageSource source, float expected) {
        pig.setHealth(pig.getMaxHealth());
        pig.invulnerableTime = 0;
        float before = pig.getHealth();
        pig.hurt(source, HIT);
        expectNear(what + ": " + expected + " of " + HIT, before - pig.getHealth(), expected);
    }

    private static void expectNear(String what, float actual, float expected) {
        checks++;
        if (Math.abs(actual - expected) <= TOLERANCE * Math.max(1.0f, expected)) {
            log("PASS " + what + " (got " + String.format("%.2f", actual) + ")");
        } else {
            failures++;
            log("FAIL " + what + " (got " + String.format("%.2f", actual) + ")");
        }
    }

    private static void expect(String what, boolean actual, boolean expected) {
        checks++;
        if (actual == expected) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what + " (got " + actual + ")");
        }
    }

    private static void expectCount(String what, int actual, int expected) {
        checks++;
        if (actual == expected) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what + " (got " + actual + ")");
        }
    }

    private static void fail(String message) {
        checks++;
        failures++;
        log("FAIL " + message);
    }

    private static void log(String message) {
        System.out.println("[damage-checks] " + message);
    }
}
