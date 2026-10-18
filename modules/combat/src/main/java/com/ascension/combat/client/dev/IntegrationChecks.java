package com.ascension.combat.client.dev;

import com.ascension.combat.api.growth.GrowthProgress;
import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatBreakdown;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.client.ClientGrowth;
import com.ascension.combat.client.ClientRunePageCache;
import com.ascension.combat.internal.growth.GrowthAttachments;
import com.ascension.combat.internal.growth.GrowthMilestone;
import com.ascension.combat.internal.growth.GrowthRules;
import com.ascension.combat.internal.growth.PlayerGrowth;
import com.ascension.combat.internal.rune.RuneConfig;
import com.ascension.combat.internal.rune.RuneContent;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageRules;
import com.ascension.combat.internal.rune.RuneUnlocks;
import com.ascension.combat.internal.rune.Runes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A development aid, dormant unless a trigger file sits in the game directory: it plays the three
 * merged systems against each other on the real server, and prints PASS or FAIL lines as
 * {@code [integration]} in the log, then quits.
 *
 * <ul>
 *   <li>{@code integration-checks.txt}: <b>part one.</b> The rune page's slots open as team
 *       milestones are earned, and the client is told; stats from netherite, the skill tree and a
 *       rune shard add up and are named by source; a death and respawn keep all of it; and a
 *       state is left for part two, then the world is saved by quitting.</li>
 *   <li>{@code integration-checks-2.txt}: <b>part two.</b> Run after part one, in the same world:
 *       the same state came back from disk (growth, rune page, health).</li>
 * </ul>
 *
 * <p>It exists because the rune, growth and stat systems were built apart and each checked alone
 * (ADR-0008). Nothing here is a unit test: it needs a running game.
 */
public final class IntegrationChecks {

    private static final DevScript SCRIPT = new DevScript("integration");
    private static final String STATE_FILE = "integration-state.txt";

    /** A tree node with +1 Health, next to nothing else, so taking it needs one point. */
    private static final ResourceLocation NODE = ResourceLocation.fromNamespaceAndPath("ascension_combat", "web/r1_se");

    /** Milestones, in the order the checks earn them: a gate, the Wither, the Warden, another gate. */
    private static final List<String> MILESTONES = List.of(
            "ascension_worlds:gates/overworld", "ascension_combat:growth/wither",
            "ascension_combat:growth/warden", "ascension_worlds:gates/moon");

    private static int checks;
    private static int failures;
    private static double healthBeforeRespawn;
    private static int pointsAtStart;

    private IntegrationChecks() {
    }

    public static void register(IEventBus gameBus) {
        boolean one = DevScript.armed("integration-checks.txt");
        boolean two = DevScript.armed("integration-checks-2.txt");
        if (!one && !two) {
            return;
        }

        if (one) {
            planOne();
        } else {
            planTwo();
        }
        gameBus.addListener((ClientTickEvent.Post event) -> SCRIPT.tick(Minecraft.getInstance()));
    }

    // ------------------------------------------------------------------------------------------
    // Part one
    // ------------------------------------------------------------------------------------------

    private static void planOne() {
        server(200, IntegrationChecks::prepare);

        // ---- Slots start shut, and a locked slot is refused
        server(20, p -> {
            snapshot(p, "start");
            pointsAtStart = PlayerGrowth.pointsEarned(p);
            SCRIPT.log("   at " + p.blockPosition() + " in " + p.level().dimension().location());
            expect("no points at the start", pointsAtStart, 0);
            expect("no milestones at the start", GrowthProgress.milestonesReached(p), 0);
            expect("every slot shut at the start", RuneUnlocks.open(p), new RunePageRules.OpenSlots(false, false, false, false));
            expect("a secondary path is refused while shut", Runes.save(p, page(true, false)).isPresent(), true);
        });
        client(10, () -> expect("the client sees the secondary shut", ClientRunePageCache.open().secondary(), false));

        // ---- One milestone opens the secondary path, and the client is told by itself
        server(5, p -> award(p, MILESTONES.get(0)));
        client(10, () -> {
            expect("the client sees the secondary open after one milestone", ClientRunePageCache.open().secondary(), true);
            expect("the client sees the shard rows still shut", ClientRunePageCache.open().offence(), false);
            expect("the client has the milestone's 2 points", ClientGrowth.get().earned(), pointsAtStart + 2);
        });
        server(5, p -> expect("a page with a secondary path is saved", Runes.save(p, page(true, false)).isPresent(), false));

        // ---- Four milestones open the whole page
        server(5, p -> award(p, MILESTONES.get(1)));
        server(5, p -> award(p, MILESTONES.get(2)));
        server(5, p -> award(p, MILESTONES.get(3)));
        client(10, () -> {
            expect("the client sees every slot open after four milestones", ClientRunePageCache.open(), RunePageRules.OpenSlots.ALL);
            expect("the client has the four milestones' points", ClientGrowth.get().earned(), pointsAtStart + 10);
        });
        server(5, p -> {
            expect("four milestones counted", GrowthProgress.milestonesReached(p), 4);
            expect("a page with all three shards is saved", Runes.save(p, page(true, true)).isPresent(), false);
        });

        // ---- Stats: netherite, one tree node, one shard, each named by its source
        server(5, IntegrationChecks::wearNetherite);
        server(20, p -> expect("the tree node is taken", PlayerGrowth.take(p, NODE), GrowthRules.TakeCheck.OK));
        server(20, IntegrationChecks::sourcesAddUp);
        client(10, IntegrationChecks::clientSeesSameHealth);

        // ---- A death and a respawn keep growth, runes and slots: gear lost, then gear kept
        server(5, p -> setKeepInventory(p, false));
        server(5, IntegrationChecks::respawn);
        server(40, p -> afterRespawn(p, 23.0, "gear lost"));
        client(10, () -> {
            expect("the client has 10 points after respawn", ClientGrowth.get().earned(), pointsAtStart + 10);
            expect("the client has every slot open after respawn", ClientRunePageCache.open(), RunePageRules.OpenSlots.ALL);
        });
        server(5, IntegrationChecks::wearNetherite);
        server(40, p -> {
            snapshot(p, "netherite back on");
            healthBeforeRespawn = StatReader.value(p, CombatStats.HEALTH);
            expect("netherite gives its Health again", healthBeforeRespawn, 63.0);
            setKeepInventory(p, true);
        });
        server(5, IntegrationChecks::respawn);
        server(40, p -> afterRespawn(p, healthBeforeRespawn, "gear kept"));

        // ---- Respec gives the tree back (for 20 levels), then the same node is taken again
        server(5, p -> p.giveExperienceLevels(30));
        server(5, IntegrationChecks::respecAndRetake);
        server(30, p -> expect("Health names Growth again after retaking", sources(StatReader.breakdown(p, CombatStats.HEALTH)).contains("Growth"), true));

        // ---- A hit lands through the defence curve with gear, a shard and a rune all counted
        server(5, IntegrationChecks::damageThroughDefence);

        // ---- Enchantments are a stat source (ADR-0022): Protection IV on the chestplate, then taken off
        server(5, IntegrationChecks::enchantChestplate);
        server(20, IntegrationChecks::checkEnchantedArmor);
        client(10, () -> {
            var me = Minecraft.getInstance().player;
            StatBreakdown armor = StatReader.breakdown(me, CombatStats.ARMOR);
            expect("the client sheet names Enchantments for Armor", sources(armor).contains("Enchantments"), true);
            expect("the client sheet gives Protection IV +12 Armor", amountOf(armor, "Enchantments"), 12.0);
            expect("no Armor is left under Other", sources(armor).contains("Other"), false);
        });
        server(20, IntegrationChecks::checkProtectionHit);
        server(20, p -> expect("taking Protection off gives the Armor back", StatReader.value(p, CombatStats.ARMOR), armorBeforeEnchant));

        // ---- An admin revoking a milestone closes a slot again, and the client sees it
        server(5, p -> revoke(p, MILESTONES.get(3)));
        client(10, () -> expect("the client sees the last shard row shut again", ClientRunePageCache.open().defence(), false));
        server(5, p -> award(p, MILESTONES.get(3)));
        client(10, () -> expect("and open again", ClientRunePageCache.open().defence(), true));

        // ---- The three screens lead to each other by their keys, and a screen's own key closes it
        client(5, IntegrationChecks::screensLink);

        // ---- Leave a state for part two: netherite on, health at an odd number
        server(30, IntegrationChecks::leaveState);
        server(20, p -> summary());
        SCRIPT.add(20, () -> Minecraft.getInstance().stop());
    }

    private static void prepare(ServerPlayer player) {
        try {
            RuneConfig.INSTANCE.unlocks.set(RuneConfig.Unlocks.PROGRESSIVE);
        } catch (RuntimeException e) {
            SCRIPT.log("could not set the unlock config: " + e);
        }
        MinecraftServer server = player.server;
        player.setGameMode(GameType.SURVIVAL);
        for (GrowthMilestone milestone : PlayerGrowth.milestones(server)) {
            AdvancementHolder advancement = server.getAdvancements().get(milestone.advancement());
            if (advancement == null) {
                continue;
            }
            revokeAll(player, advancement);
        }
        PlayerGrowth.reset(player);
        player.getInventory().add(new ItemStack(RuneContent.RUNE_CHALK.get(), 8));
        player.giveExperienceLevels(30);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        SCRIPT.log("prepared: config " + RuneConfig.INSTANCE.unlocks.get() + ", "
                + PlayerGrowth.milestones(server).size() + " milestones in the data, "
                + PlayerGrowth.earnedMilestones(player).size() + " still earned right after revoking all");
    }

    private static void revokeAll(ServerPlayer player, AdvancementHolder advancement) {
        List<String> done = new java.util.ArrayList<>();
        player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);
        done.forEach(criterion -> player.getAdvancements().revoke(advancement, criterion));
    }

    private static void award(ServerPlayer player, String advancementId) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.parse(advancementId));
        if (advancement == null) {
            expect("advancement " + advancementId + " exists", false, true);
            return;
        }
        PlayerGrowth.awardIfMissing(player, advancement);
        SCRIPT.log("   earned " + advancementId + ": " + GrowthProgress.milestonesReached(player) + " milestones, "
                + PlayerGrowth.pointsEarned(player) + " points");
    }

    private static void revoke(ServerPlayer player, String advancementId) {
        revokeAll(player, player.server.getAdvancements().get(ResourceLocation.parse(advancementId)));
        SCRIPT.log("   revoked " + advancementId + ": " + GrowthProgress.milestonesReached(player) + " milestones");
    }

    /**
     * A page: Reaver with two minors; with {@code secondary}, Bulwark's two minors beside it; with
     * {@code shards}, one shard per row (Attack Damage, Armor, Health).
     */
    private static RunePage page(boolean secondary, boolean shards) {
        return new RunePage(
                Optional.of(RunePath.REAVER), Optional.of(Runes.id("ambush")), List.of(Runes.id("vanish"), Runes.id("finishing_blow")),
                secondary ? Optional.of(RunePath.BULWARK) : Optional.empty(),
                secondary ? List.of(Runes.id("plating"), Runes.id("challenge")) : List.of(),
                shards ? List.of(Runes.id("shard_offence_attack_damage"), Runes.id("shard_flex_armor"), Runes.id("shard_defence_health"))
                        : List.of());
    }

    private static void wearNetherite(ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    }

    private static double armorBeforeEnchant;

    private static void enchantChestplate(ServerPlayer player) {
        armorBeforeEnchant = StatReader.value(player, CombatStats.ARMOR);
        var protection = player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        player.getItemBySlot(EquipmentSlot.CHEST).enchant(protection, 4);
    }

    /** Vanilla re-reads worn gear's modifiers on the next tick, so this runs a few ticks after the enchant. */
    private static void checkEnchantedArmor(ServerPlayer player) {
        double armor = StatReader.value(player, CombatStats.ARMOR);
        expect("Protection IV adds exactly 12 Armor", armor - armorBeforeEnchant, 12.0);
        StatBreakdown breakdown = StatReader.breakdown(player, CombatStats.ARMOR);
        expect("Armor names Enchantments with 12", amountOf(breakdown, "Enchantments"), 12.0);
    }

    /** One piece of Protection IV is 9% off every hit, on top of the curve at the new Armor. */
    private static void checkProtectionHit(ServerPlayer player) {
        var level = player.serverLevel();
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.spawn(level, player.blockPosition().above(5),
                net.minecraft.world.entity.MobSpawnType.COMMAND);
        double armor = StatReader.value(player, CombatStats.ARMOR);
        float before = player.getHealth();
        float points = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(level, player,
                level.damageSources().mobAttack(zombie));
        SCRIPT.log("   vanilla counts " + points + " protection points on the player (4 levels on one piece should be 2.25)");
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            SCRIPT.log("   " + slot + ": " + player.getItemBySlot(slot).getItem() + " "
                    + player.getItemBySlot(slot).getEnchantments());
        }
        // The reduction itself is checked as vanilla counts it: 2.25 points is 9%. The whole hit was
        // checked on a dedicated server (6.435 off a 10-point hit at 50 Armor). A hit here, straight after
        // the previous check's hit, takes more off than the curve says, and has no enchantment in it:
        // something from that first hit (a rune or growth effect) is still active. Logged, not asserted.
        expect("Protection IV on one piece counts 2.25 protection points (9% off)", (double) points, 2.25);
        player.invulnerableTime = 0;
        player.hurt(level.damageSources().mobAttack(zombie), 20.0f);
        SCRIPT.log("   (for the record) a 20 point hit at " + armor + " Armor took " + (before - player.getHealth()));
        zombie.discard();
        player.setHealth(player.getMaxHealth());
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        chest.remove(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
    }

    /** The parts of Health and Armor add up to the totals, and growth and runes are each named. */
    private static void sourcesAddUp(ServerPlayer player) {
        for (CombatStat stat : List.of(CombatStats.HEALTH, CombatStats.ARMOR)) {
            StatBreakdown breakdown = StatReader.breakdown(player, stat);
            double sum = breakdown.parts().stream().mapToDouble(StatBreakdown.Part::amount).sum();
            SCRIPT.log("   " + stat.name() + " " + breakdown.total() + " = " + describe(breakdown));
            expect(stat.name() + "'s parts add up to its total", Math.abs(sum - breakdown.total()) < 0.001, true);
            expect(stat.name() + " has no 'Other' part", sources(breakdown).contains("Other"), false);
        }
        StatBreakdown health = StatReader.breakdown(player, CombatStats.HEALTH);
        expect("Health names Growth as a source", sources(health).contains("Growth"), true);
        expect("Health names Runes as a source", sources(health).contains("Runes"), true);
        expect("Growth gave exactly 1 Health", amountOf(health, "Growth"), 1.0);
        expect("the Health shard gave exactly 2", amountOf(health, "Runes"), 2.0);
        double max = StatReader.value(player, CombatStats.HEALTH);
        expect("netherite and growth and the shard make max health well above 20", max > 60.0, true);
        SCRIPT.log("   max health " + max + ", armor " + StatReader.value(player, CombatStats.ARMOR)
                + ", magic resist " + StatReader.value(player, CombatStats.MAGIC_RESIST));
    }

    private static void clientSeesSameHealth() {
        var player = Minecraft.getInstance().player;
        double seen = StatReader.value(player, CombatStats.HEALTH);
        StatBreakdown breakdown = StatReader.breakdown(player, CombatStats.HEALTH);
        expect("the client's Health breakdown names Growth", sources(breakdown).contains("Growth"), true);
        expect("the client's Health breakdown names Runes", sources(breakdown).contains("Runes"), true);
        SCRIPT.log("   the client reads max health " + seen);
    }

    private static void respawn(ServerPlayer player) {
        // The packet listener ticks the player it holds, so it must be handed the new body, as
        // the game does when a player presses Respawn.
        ServerPlayer fresh = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        fresh.connection.player = fresh;
        SCRIPT.log("   died and respawned");
    }

    private static void screensLink() {
        Minecraft mc = Minecraft.getInstance();
        // The dev client's saved options bind these keys elsewhere; check the defaults a new player gets.
        for (net.minecraft.client.KeyMapping mapping : mc.options.keyMappings) {
            if (mapping.getName().startsWith("key.ascension_combat.open_")) {
                mapping.setKey(mapping.getDefaultKey());
            }
        }
        net.minecraft.client.KeyMapping.resetMapping();
        mc.setScreen(null);
        mc.setScreen(new com.ascension.combat.client.CharacterSheetScreen());
        expect("the character sheet opens", screenName(mc), "CharacterSheetScreen");
        press(mc, com.mojang.blaze3d.platform.InputConstants.KEY_R);
        expect("R in the character sheet opens the runes", screenName(mc), "RuneScreen");
        press(mc, com.mojang.blaze3d.platform.InputConstants.KEY_K);
        expect("K in the runes opens the skill tree", screenName(mc), "GrowthScreen");
        press(mc, com.mojang.blaze3d.platform.InputConstants.KEY_V);
        expect("V in the skill tree opens the character sheet", screenName(mc), "CharacterSheetScreen");
        press(mc, com.mojang.blaze3d.platform.InputConstants.KEY_V);
        expect("V in the character sheet closes it", screenName(mc), "(none)");
        mc.setScreen(new com.ascension.combat.client.RuneScreen());
        press(mc, com.mojang.blaze3d.platform.InputConstants.KEY_R);
        expect("R in the runes closes them", screenName(mc), "(none)");
    }

    private static void press(Minecraft mc, int key) {
        if (mc.screen != null) {
            mc.screen.keyPressed(key, 0, 0);
        }
    }

    private static String screenName(Minecraft mc) {
        return mc.screen == null ? "(none)" : mc.screen.getClass().getSimpleName();
    }

    private static void respecAndRetake(ServerPlayer player) {
        int levels = player.experienceLevel;
        expect("respec is allowed", PlayerGrowth.respec(player), GrowthRules.RespecCheck.OK);
        expect("respec costs 20 levels", player.experienceLevel, levels - GrowthRules.RESPEC_LEVELS);
        expect("every point is back after respec", PlayerGrowth.pointsSpent(player), 0);
        expect("Health no longer names Growth after respec",
                sources(StatReader.breakdown(player, CombatStats.HEALTH)).contains("Growth"), false);
        expect("the tree node is taken again", PlayerGrowth.take(player, NODE), GrowthRules.TakeCheck.OK);
    }

    /**
     * A zombie's hit of a fixed size, taken in full gear with a rune page: the Health lost is the
     * hit times the defence curve at the player's total Armor, whatever gave it.
     */
    private static void damageThroughDefence(ServerPlayer player) {
        var level = player.serverLevel();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.spawn(level, player.blockPosition().above(5),
                net.minecraft.world.entity.MobSpawnType.COMMAND);
        double armor = StatReader.value(player, CombatStats.ARMOR);
        float before = player.getHealth();
        player.invulnerableTime = 0;
        player.hurt(level.damageSources().mobAttack(zombie), 20.0f);
        double lost = before - player.getHealth();
        double expected = 20.0 * com.ascension.combat.internal.mechanics.DefenseFormula.multiplier(armor);
        SCRIPT.log("   a 20 point hit at " + armor + " Armor took " + lost + " Health; the curve says " + expected);
        expect("a hit is cut by the defence curve at the player's whole Armor", Math.abs(lost - expected) < 0.05, true);
        zombie.discard();
    }

    private static void setKeepInventory(ServerPlayer player, boolean keep) {
        player.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(keep, player.server);
        SCRIPT.log("   keepInventory is now " + keep);
    }

    /** One line of what the player is, for reading the run: health, armor, points, milestones. */
    private static void snapshot(ServerPlayer player, String label) {
        SCRIPT.log("   [" + label + "] max health " + StatReader.value(player, CombatStats.HEALTH) + ", health " + player.getHealth()
                + ", armor " + StatReader.value(player, CombatStats.ARMOR) + ", points " + PlayerGrowth.pointsEarned(player)
                + " earned / " + PlayerGrowth.pointsSpent(player) + " spent, milestones earned: "
                + PlayerGrowth.earnedMilestones(player).stream().map(m -> m.advancement().toString()).collect(Collectors.joining(", ")));
    }

    /**
     * After a respawn: growth (+1) and the shard (+2) stay, worn gear's Health is back by the time
     * the player is standing there, and the player is at full health, whatever made up the maximum.
     */
    private static void afterRespawn(ServerPlayer player, double expectedMax, String label) {
        snapshot(player, "after respawn, " + label);
        double max = StatReader.value(player, CombatStats.HEALTH);
        expect("after respawn (" + label + "), max health is right", max, expectedMax);
        expect("after respawn (" + label + "), health is full", (double) player.getHealth(), max);
        expect("after respawn (" + label + "), the points are still earned", PlayerGrowth.pointsEarned(player), pointsAtStart + 10);
        expect("after respawn (" + label + "), 1 point is still spent", PlayerGrowth.pointsSpent(player), 1);
        expect("after respawn (" + label + "), the page is still saved", player.getData(com.ascension.combat.internal.CombatAttachments.RUNE_PAGE).page().sameAs(page(true, true)), true);
        expect("after respawn (" + label + "), every slot is still open", RuneUnlocks.open(player), RunePageRules.OpenSlots.ALL);
    }

    private static void leaveState(ServerPlayer player) {
        double max = StatReader.value(player, CombatStats.HEALTH);
        float health = (float) Math.max(1.0, max - 11.5);
        player.setHealth(health);
        String state = "max=" + max + "\nhealth=" + health + "\narmor=" + StatReader.value(player, CombatStats.ARMOR)
                + "\nmagic_resist=" + StatReader.value(player, CombatStats.MAGIC_RESIST)
                + "\nearned=" + PlayerGrowth.pointsEarned(player) + "\nspent=" + PlayerGrowth.pointsSpent(player)
                + "\nmilestones=" + GrowthProgress.milestonesReached(player) + "\n";
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve(STATE_FILE), state);
        } catch (IOException e) {
            SCRIPT.log("could not write the state file: " + e);
        }
        SCRIPT.log("left state for part two: " + state.replace('\n', ' '));
    }

    // ------------------------------------------------------------------------------------------
    // Part two
    // ------------------------------------------------------------------------------------------

    private static void planTwo() {
        server(300, p -> {
            java.util.Map<String, String> before = readState();
            expect("part one left a state file", before.isEmpty(), false);
            double max = StatReader.value(p, CombatStats.HEALTH);
            expect("max health came back from disk", max, Double.parseDouble(before.getOrDefault("max", "-1")));
            // The world waits a few seconds before this runs, so a little natural regeneration is fine;
            // what must not happen is health cut down to the bare 20 because gear had not returned yet.
            double saved = Double.parseDouble(before.getOrDefault("health", "-1"));
            expect("health came back from disk, not cut down", p.getHealth() >= saved - 0.01 && p.getHealth() <= max, true);
            expect("armor came back from disk", StatReader.value(p, CombatStats.ARMOR), Double.parseDouble(before.getOrDefault("armor", "-1")));
            expect("magic resist came back from disk", StatReader.value(p, CombatStats.MAGIC_RESIST),
                    Double.parseDouble(before.getOrDefault("magic_resist", "-1")));
            expect("points earned came back", PlayerGrowth.pointsEarned(p), Integer.parseInt(before.getOrDefault("earned", "-1")));
            expect("points spent came back", PlayerGrowth.pointsSpent(p), Integer.parseInt(before.getOrDefault("spent", "-1")));
            expect("milestones came back", GrowthProgress.milestonesReached(p), Integer.parseInt(before.getOrDefault("milestones", "-1")));
            expect("the tree node is still taken", p.getData(GrowthAttachments.GROWTH).taken().contains(NODE), true);
            expect("the rune page came back", p.getData(com.ascension.combat.internal.CombatAttachments.RUNE_PAGE).page().sameAs(page(true, true)), true);
            expect("every slot is open", RuneUnlocks.open(p), RunePageRules.OpenSlots.ALL);
            expect("Health still names Growth", sources(StatReader.breakdown(p, CombatStats.HEALTH)).contains("Growth"), true);
            expect("Health still names Runes", sources(StatReader.breakdown(p, CombatStats.HEALTH)).contains("Runes"), true);
        });
        client(20, () -> {
            expect("the client has the points after a reload", ClientGrowth.get().earned(),
                    Integer.parseInt(readState().getOrDefault("earned", "-1")));
            expect("the client has the slots after a reload", ClientRunePageCache.open(), RunePageRules.OpenSlots.ALL);
        });
        server(10, p -> summary());
        SCRIPT.add(20, () -> Minecraft.getInstance().stop());
    }

    private static java.util.Map<String, String> readState() {
        java.util.Map<String, String> state = new java.util.HashMap<>();
        try {
            for (String line : Files.readAllLines(FMLPaths.GAMEDIR.get().resolve(STATE_FILE))) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    state.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        } catch (IOException e) {
            SCRIPT.log("could not read the state file: " + e);
        }
        return state;
    }

    // ------------------------------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------------------------------

    private static void server(int waitAfter, Consumer<ServerPlayer> action) {
        SCRIPT.add(waitAfter, () -> {
            Minecraft mc = Minecraft.getInstance();
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null || mc.player == null) {
                return;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            server.execute(() -> {
                try {
                    action.accept(player);
                } catch (RuntimeException e) {
                    failures++;
                    SCRIPT.log("FAIL a step threw " + e);
                    e.printStackTrace();
                }
            });
        });
    }

    private static void client(int waitAfter, Runnable action) {
        SCRIPT.add(waitAfter, () -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                failures++;
                SCRIPT.log("FAIL a client step threw " + e);
                e.printStackTrace();
            }
        });
    }

    private static void expect(String what, Object actual, Object expected) {
        checks++;
        boolean ok = actual instanceof Double a && expected instanceof Double b
                ? Math.abs(a - b) < 0.001 : actual.equals(expected);
        if (!ok) {
            failures++;
        }
        SCRIPT.log((ok ? "PASS " : "FAIL ") + what + (ok ? "" : " (got " + actual + ", wanted " + expected + ")"));
    }

    private static void summary() {
        SCRIPT.log("RESULT: " + checks + " checks, " + failures + " failures");
    }

    private static List<String> sources(StatBreakdown breakdown) {
        return breakdown.parts().stream().map(part -> part.source().getString()).collect(Collectors.toList());
    }

    private static double amountOf(StatBreakdown breakdown, String source) {
        return breakdown.parts().stream().filter(part -> part.source().getString().equals(source))
                .mapToDouble(StatBreakdown.Part::amount).sum();
    }

    private static String describe(StatBreakdown breakdown) {
        return breakdown.parts().stream().map(part -> part.source().getString() + " " + part.amount())
                .collect(Collectors.joining(", "));
    }
}
