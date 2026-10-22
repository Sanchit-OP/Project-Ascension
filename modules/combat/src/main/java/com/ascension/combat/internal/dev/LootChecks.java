package com.ascension.combat.internal.dev;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A development aid, dormant unless a file named {@code loot-checks.txt} sits in the game directory: on a dedicated
 * server it fills chest loot tables the way the game and Lootr both do ({@code LootTable.fill} with a player and a
 * position, docs/progression/loot.md) in Earth and on each planet, and reads what came out. Prints {@code PASS} or
 * {@code FAIL} lines tagged {@code [loot-checks]} and stops the server.
 */
public final class LootChecks {

    private static final String TRIGGER = "loot-checks.txt";
    private static final int FILLS = 400;
    private static final ResourceLocation SIMPLE_DUNGEON = ResourceLocation.withDefaultNamespace("chests/simple_dungeon");
    private static final ResourceLocation END_CITY = ResourceLocation.withDefaultNamespace("chests/end_city_treasure");

    private static MinecraftServer server;
    private static int settle;
    private static boolean armed;
    private static boolean finished;
    private static int checks;
    private static int failures;

    private LootChecks() {
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
        gameBus.addListener(LootChecks::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed || finished || server == null) {
            return;
        }
        settle++;
        if (settle == 200) {
            try {
                run();
            } catch (RuntimeException e) {
                fail("the checks threw " + e);
                e.printStackTrace();
            }
            finished = true;
            log("RESULT: " + checks + " checks, " + failures + " failures");
            server.halt(false);
        }
    }

    /** What a pile of filled chests held, by the kinds a player cares about. */
    private static final class Tally {
        int chests;
        int stacks;
        int junk;
        int mildJunk;
        int titanium;
        int helium;
        int diamonds;
        int components;
        int books;
        int experience;
        int netherite;

        void add(ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }
            stacks++;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String name = id.toString();
            if (name.endsWith("_seeds") || name.equals("minecraft:rotten_flesh") || name.equals("minecraft:poisonous_potato")) {
                mildJunk++;
            }
            if (name.equals("minecraft:wheat") || name.equals("minecraft:bone") || name.equals("minecraft:rotten_flesh")
                    || name.endsWith("_seeds") || name.equals("minecraft:bread")) {
                junk++;
            } else if (name.equals("ascension_worlds:raw_titanium")) {
                titanium++;
            } else if (name.equals("ascension_worlds:raw_helium")) {
                helium++;
            } else if (stack.is(Items.DIAMOND)) {
                diamonds++;
            } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
                experience++;
            } else if (stack.is(Items.ENCHANTED_BOOK)) {
                books++;
            } else if (stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.NETHERITE_INGOT)) {
                netherite++;
            } else if (id.getNamespace().equals("ascension_combat")) {
                components++;
            }
        }

        String describe() {
            return String.format("%.1f stacks a chest; per 100 chests: %.0f junk, %.0f raw titanium, %.0f raw helium, %.0f diamonds, "
                            + "%.0f experience bottles, %.0f netherite, %.0f enchanted books, %.0f build-path components",
                    stacks / (double) chests, per100(junk), per100(titanium), per100(helium), per100(diamonds), per100(experience),
                    per100(netherite), per100(books), per100(components));
        }

        private double per100(int n) {
            return 100.0 * n / chests;
        }
    }

    private static ServerLevel level(String dimension) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
        if (level == null) {
            fail("the dimension " + dimension + " is not loaded");
        }
        return level;
    }

    private static LootParams chestParams(ServerLevel level, ServerPlayer player) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(level.getSharedSpawnPos()))
                .withLuck(player.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
    }

    private static Tally fill(ServerLevel level, ServerPlayer player, ResourceLocation tableId) {
        LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, tableId));
        Tally tally = new Tally();
        for (int i = 0; i < FILLS; i++) {
            SimpleContainer chest = new SimpleContainer(54);
            table.fill(chest, chestParams(level, player), 1000L + i);
            tally.chests++;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                tally.add(chest.getItem(slot));
            }
        }
        return tally;
    }

    private static void run() {
        ServerLevel earth = level("minecraft:overworld");
        ServerLevel moon = level("ascension_worlds:moon");
        ServerLevel nether = level("minecraft:the_nether");
        ServerLevel end = level("minecraft:the_end");
        if (earth == null || moon == null || nether == null || end == null) {
            return;
        }
        ServerPlayer player = FakePlayerFactory.get(earth, new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000100f"), "[loot-checks]"));

        log("--- chests on Earth lose only the mild junk and gain nothing");
        Tally earthDungeon = fill(earth, player, SIMPLE_DUNGEON);
        log("Earth, simple_dungeon: " + earthDungeon.describe());
        expect("Earth's dungeon chests hold no seeds, rotten flesh or poisonous potatoes (the mild strip)", earthDungeon.mildJunk == 0, true);
        expect("but still hold wheat, bread and bones, which are food and bone meal", earthDungeon.junk > 0, true);
        expect("and none of the planets' loot: no raw titanium, raw helium, build-path components or diamonds",
                earthDungeon.titanium == 0 && earthDungeon.helium == 0 && earthDungeon.components == 0 && earthDungeon.diamonds == 0, true);

        log("--- the Moon");
        Tally moonDungeon = fill(moon, player, SIMPLE_DUNGEON);
        Tally moonCity = fill(moon, player, END_CITY);
        log("Moon, simple_dungeon (the Ruined Citadel's): " + moonDungeon.describe());
        log("Moon, end_city_treasure (the Ruined Citadel's): " + moonCity.describe());
        expect("a Moon dungeon chest holds no wheat, bones, seeds or rotten flesh", moonDungeon.junk == 0, true);
        expect("it holds raw titanium and raw helium", moonDungeon.titanium > 0 && moonDungeon.helium > 0, true);
        expect("it holds diamonds, experience bottles, enchanted books and build-path components",
                moonDungeon.diamonds > 0 && moonDungeon.experience > 0 && moonDungeon.books > 0 && moonDungeon.components > 0, true);
        expect("a Moon chest holds more than Earth's did", moonDungeon.stacks > earthDungeon.stacks - earthDungeon.junk, true);

        log("--- the Nether");
        Tally netherDungeon = fill(nether, player, SIMPLE_DUNGEON);
        log("Nether, simple_dungeon: " + netherDungeon.describe());
        expect("a Nether chest holds no junk", netherDungeon.junk == 0, true);
        expect("it holds netherite, diamonds, books and components", netherDungeon.netherite > 0 && netherDungeon.diamonds > 0
                && netherDungeon.books > 0 && netherDungeon.components > 0, true);

        log("--- the End");
        Tally endCity = fill(end, player, END_CITY);
        Tally endDungeon = fill(end, player, SIMPLE_DUNGEON);
        log("End, end_city_treasure: " + endCity.describe());
        log("End, simple_dungeon: " + endDungeon.describe());
        expect("an End chest holds no junk", endDungeon.junk == 0, true);
        expect("it holds netherite, diamonds, books and components", endDungeon.netherite > 0 && endDungeon.diamonds > 0
                && endDungeon.books > 0 && endDungeon.components > 0, true);
        expect("the planets rise: the End's chests hold more diamonds than the Moon's", endDungeon.diamonds > moonDungeon.diamonds, true);

        log("--- only chests");
        LootTable zombieTable = server.reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("entities/zombie")));
        Zombie zombie = EntityType.ZOMBIE.create(moon);
        int flesh = 0;
        int extras = 0;
        for (int i = 0; i < FILLS; i++) {
            LootParams params = new LootParams.Builder(moon)
                    .withParameter(LootContextParams.THIS_ENTITY, zombie)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(moon.getSharedSpawnPos()))
                    .withParameter(LootContextParams.DAMAGE_SOURCE, moon.damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            for (ItemStack stack : zombieTable.getRandomItems(params)) {
                String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (name.equals("minecraft:rotten_flesh")) {
                    flesh++;
                } else if (name.equals("ascension_worlds:raw_helium") || name.equals("minecraft:diamond")) {
                    extras++;
                }
            }
        }
        expect("a zombie on the Moon still drops rotten flesh (the junk strip is for chests)", flesh > 0, true);
        expect("and drops none of the planet loot", extras == 0, true);

        log("--- a real chest block on the Moon, opened the way a player opens one");
        BlockPos pos = moon.getSharedSpawnPos().above(3);
        moon.getChunk(pos);
        moon.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (moon.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, SIMPLE_DUNGEON), 42L);
            chest.unpackLootTable(player);
            int stacks = 0;
            int junk = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (!stack.isEmpty()) {
                    stacks++;
                    if (stack.is(Items.WHEAT) || stack.is(Items.BONE) || stack.is(Items.ROTTEN_FLESH)) {
                        junk++;
                    }
                }
            }
            expect("the placed chest filled with " + stacks + " stacks", stacks > 0, true);
            expect("and none of them junk", junk == 0, true);
        } else {
            fail("no chest block entity at the test position");
        }
        moon.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        zombie.discard();
    }

    private static void expect(String what, boolean actual, boolean expected) {
        checks++;
        if (actual == expected) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what);
        }
    }

    private static void fail(String message) {
        checks++;
        failures++;
        log("FAIL " + message);
    }

    private static void log(String message) {
        System.out.println("[loot-checks] " + message);
    }
}
