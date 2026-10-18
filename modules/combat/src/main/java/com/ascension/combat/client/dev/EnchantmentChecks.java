package com.ascension.combat.client.dev;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatBreakdown;
import com.ascension.combat.api.stat.StatReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import static com.ascension.combat.client.dev.IntegrationChecks.SCRIPT;
import static com.ascension.combat.client.dev.IntegrationChecks.amountOf;
import static com.ascension.combat.client.dev.IntegrationChecks.client;
import static com.ascension.combat.client.dev.IntegrationChecks.expect;
import static com.ascension.combat.client.dev.IntegrationChecks.server;
import static com.ascension.combat.client.dev.IntegrationChecks.sources;

/**
 * The enchantment steps of the scripted integration run (docs/progression/enchantments.md, ADR-0022):
 * enchanted gear shows its stat under "Enchantments" on the server and on the client sheet, the data
 * enchantments are offered by the table, trades and loot, and Respiration grows the lungs. Added to the
 * plan by {@link IntegrationChecks}; shares its script and counters.
 */
final class EnchantmentChecks {

    private EnchantmentChecks() {
    }

    static void plan() {
        // ---- Enchantments are a stat source (ADR-0022): Protection IV on the chestplate, then taken off
        server(5, EnchantmentChecks::enchantChestplate);
        server(20, EnchantmentChecks::checkEnchantedArmor);
        client(10, () -> {
            var me = Minecraft.getInstance().player;
            StatBreakdown armor = StatReader.breakdown(me, CombatStats.ARMOR);
            expect("the client sheet names Enchantments for Armor", sources(armor).contains("Enchantments"), true);
            expect("the client sheet gives Protection IV +12 Armor", amountOf(armor, "Enchantments"), 12.0);
            expect("no Armor is left under Other", sources(armor).contains("Other"), false);
        });
        server(20, EnchantmentChecks::checkProtectionPoints);
        server(20, p -> expect("taking Protection off gives the Armor back", StatReader.value(p, CombatStats.ARMOR), armorBeforeEnchant));

        // ---- Sharpness IV is +2 Attack Damage and Breach IV is +16% Armor Penetration, in the main hand
        server(5, p -> holdEnchanted(p, Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 4));
        server(20, p -> expectEnchanted(p, CombatStats.ATTACK_DAMAGE, "Sharpness IV", 2.0));
        client(10, () -> expect("the client sheet gives Sharpness IV +2 Attack Damage",
                amountOf(StatReader.breakdown(Minecraft.getInstance().player, CombatStats.ATTACK_DAMAGE), "Enchantments"), 2.0));
        server(5, p -> holdEnchanted(p, Items.MACE, Enchantments.BREACH, 4));
        server(20, p -> expectEnchanted(p, CombatStats.ARMOR_PENETRATION, "Breach IV", 0.16));
        server(5, p -> p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));

        // ---- Our own enchantments (data), each a stat under Enchantments; the table, trades and loot offer them
        server(5, EnchantmentChecks::holdOurEnchantedGear);
        server(20, p -> {
            expectEnchanted(p, CombatStats.LETHALITY, "Keen Edge III", 6.0);
            expectEnchanted(p, CombatStats.MAGIC_RESIST, "Warding IV", 12.0);
            expectEnchanted(p, CombatStats.HEALTH, "Vitality III", 3.0);
            expectEnchanted(p, CombatStats.MOVE_SPEED, "Fleet III", 0.1 * 0.09);
            expectEnchanted(p, CombatStats.OMNIVAMP, "Leech III", 0.03);
        });
        server(5, EnchantmentChecks::ourEnchantmentsAreOffered);
        server(5, p -> {
            p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
                strip(p, slot);
            }
        });

        // ---- Respiration grows lung capacity (the atmosphere module's rule), asked through its own command
        server(5, p -> {
            lungsWithoutRespiration = lungCapacity(p);
            SCRIPT.log("   lung capacity without Respiration: " + lungsWithoutRespiration + " units");
            enchant(p, p.getItemBySlot(EquipmentSlot.HEAD), Enchantments.RESPIRATION, 3);
        });
        server(20, p -> expect("Respiration III adds 30 seconds (120 units) to the lungs",
                lungCapacity(p) - lungsWithoutRespiration, 120));
        server(5, p -> strip(p, EquipmentSlot.HEAD));
        server(20, p -> expect("taking Respiration off gives the lungs back", lungCapacity(p), lungsWithoutRespiration));

    }

    private static double armorBeforeEnchant;
    private static int lungsWithoutRespiration;

    /**
     * Takes every enchantment off a worn or held item by swapping in a fresh copy of the item. (Removing the
     * enchantments component in place left a stack that would not take a new enchantment: seen in this
     * script for the chestplate and the helmet, 2026-10-18.)
     */
    private static void strip(ServerPlayer player, EquipmentSlot slot) {
        player.setItemSlot(slot, new ItemStack(player.getItemBySlot(slot).getItem()));
    }

    private static ResourceKey<Enchantment> ours(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("ascension_combat", name));
    }

    /** Leech III in hand (Keen Edge goes on the same sword below), Warding IV, Vitality III and Fleet III worn. */
    private static void holdOurEnchantedGear(ServerPlayer player) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        enchant(player, sword, ours("keen_edge"), 3);
        // Leech and Keen Edge are not exclusive, so one sword takes both.
        enchant(player, sword, ours("leech"), 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        enchant(player, player.getItemBySlot(EquipmentSlot.CHEST), ours("warding"), 4);
        enchant(player, player.getItemBySlot(EquipmentSlot.HEAD), ours("vitality"), 3);
        enchant(player, player.getItemBySlot(EquipmentSlot.FEET), ours("fleet"), 3);
    }

    /** In the enchanting table's pool, on traders and in loot; and Keen Edge and Warding refuse their rivals. */
    private static void ourEnchantmentsAreOffered(ServerPlayer player) {
        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (String name : List.of("keen_edge", "leech", "warding", "vitality", "fleet")) {
            var holder = registry.getHolderOrThrow(ours(name));
            expect(name + " is in the enchanting table pool", holder.is(EnchantmentTags.IN_ENCHANTING_TABLE), true);
            expect(name + " is sold by villagers", holder.is(EnchantmentTags.TRADEABLE), true);
            expect(name + " can turn up in loot", holder.is(EnchantmentTags.ON_RANDOM_LOOT), true);
        }
        expect("Keen Edge refuses Sharpness", Enchantment.areCompatible(registry.getHolderOrThrow(ours("keen_edge")),
                registry.getHolderOrThrow(Enchantments.SHARPNESS)), false);
        expect("Warding refuses Protection", Enchantment.areCompatible(registry.getHolderOrThrow(ours("warding")),
                registry.getHolderOrThrow(Enchantments.PROTECTION)), false);
        expect("Vitality takes Protection", Enchantment.areCompatible(registry.getHolderOrThrow(ours("vitality")),
                registry.getHolderOrThrow(Enchantments.PROTECTION)), true);
    }

    /**
     * The player's lung capacity in units, read from {@code /ascension atmosphere query} so combat names
     * no atmosphere class (Tier 1 modules never depend on each other, ADR-0011). The line reads
     * "lungs 80/80   carried ...": the number after the slash.
     */
    private static int lungCapacity(ServerPlayer player) {
        StringBuilder output = new StringBuilder();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                output.append(message.getString()).append('\n');
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        player.server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withSource(capture).withPermission(4), "ascension atmosphere query");
        Matcher lungs = Pattern.compile("lungs \\d+/(\\d+)").matcher(output);
        if (!lungs.find()) {
            SCRIPT.log("   no lungs line in: " + output);
            return -1;
        }
        return Integer.parseInt(lungs.group(1));
    }

    /** Enchants a stack in place. Vanilla re-reads worn or held modifiers on the next tick, so read stats a few ticks later. */
    private static void enchant(ServerPlayer player, ItemStack stack, ResourceKey<Enchantment> enchantment, int level) {
        stack.enchant(player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(enchantment), level);
    }

    private static void holdEnchanted(ServerPlayer player, Item item, ResourceKey<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        enchant(player, stack, enchantment, level);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
    }

    private static void expectEnchanted(ServerPlayer player, CombatStat stat, String what, double amount) {
        StatBreakdown breakdown = StatReader.breakdown(player, stat);
        expect(what + " gives " + stat.name() + " exactly " + amount + " under Enchantments", amountOf(breakdown, "Enchantments"), amount);
        expect(what + " leaves no " + stat.name() + " under Other", sources(breakdown).contains("Other"), false);
    }

    private static void enchantChestplate(ServerPlayer player) {
        armorBeforeEnchant = StatReader.value(player, CombatStats.ARMOR);
        enchant(player, player.getItemBySlot(EquipmentSlot.CHEST), Enchantments.PROTECTION, 4);
    }

    /** Runs a few ticks after the enchant (see {@link #enchant}). */
    private static void checkEnchantedArmor(ServerPlayer player) {
        double armor = StatReader.value(player, CombatStats.ARMOR);
        expect("Protection IV adds exactly 12 Armor", armor - armorBeforeEnchant, 12.0);
        StatBreakdown breakdown = StatReader.breakdown(player, CombatStats.ARMOR);
        expect("Armor names Enchantments with 12", amountOf(breakdown, "Enchantments"), 12.0);
    }

    /**
     * One piece of Protection IV is 2.25 protection points, which vanilla turns into 9% off. Checked as
     * vanilla counts it; the whole hit through the curve was checked on a dedicated server. (A player hit
     * straight after another took more off than the curve says with no enchantment involved, so the hit
     * is not asserted here: see enchantments.md section 3.5.) Then Protection is taken off.
     */
    private static void checkProtectionPoints(ServerPlayer player) {
        float points = EnchantmentHelper.getDamageProtection(player.serverLevel(), player,
                player.serverLevel().damageSources().generic());
        expect("Protection IV on one piece counts 2.25 protection points (9% off)", (double) points, 2.25);
        strip(player, EquipmentSlot.CHEST);
    }

}
