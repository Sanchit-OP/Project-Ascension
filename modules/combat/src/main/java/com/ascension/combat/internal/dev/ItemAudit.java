package com.ascension.combat.internal.dev;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A development aid, dormant unless a file named {@code item-audit.txt} sits in the game directory: on a dedicated
 * server it asks every registered item for the attribute modifiers it really gives (the same
 * {@code ItemStack.getAttributeModifiers()} the game, the tooltips and mobs read, so our data, the conversions and
 * other mods' own numbers are all in it), sorts the weapons, bows, armour and off-hand pieces out, and writes
 * {@code item-audit.csv} (one row each, the stats a player ends up with) and {@code item-audit-recipes.csv} (every
 * recipe that makes one of them out of another that is stronger than the result). Prints {@code [item-audit]} lines
 * and stops the server.
 *
 * <p>It measures; it does not pass or fail. {@code docs/progression/item-audit.md} says what the numbers showed.
 */
public final class ItemAudit {

    private static final String TRIGGER = "item-audit.txt";
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static MinecraftServer server;
    private static int settle;
    private static boolean armed;
    private static boolean finished;

    private ItemAudit() {
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
        gameBus.addListener(ItemAudit::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed || finished || server == null) {
            return;
        }
        settle++;
        if (settle == 200) {
            try {
                run();
            } catch (IOException | RuntimeException e) {
                log("the audit threw " + e);
                e.printStackTrace();
            }
            finished = true;
            server.halt(false);
        }
    }

    /** What an item gives in one slot, as a player ends up with it. */
    private static final class Row {
        ResourceLocation id;
        String itemClass;
        String category;
        EquipmentSlot slot;
        final Map<String, Double> stats = new LinkedHashMap<>();
        double vanillaArmor;
        double vanillaToughness;
        final List<String> other = new ArrayList<>();
        boolean any;

        double stat(String name) {
            return stats.getOrDefault(name, 0.0);
        }

        /** Attack Damage as a tooltip writes it: a bare hand's 1 plus what the item adds. */
        double attack() {
            return 1.0 + stat("attack_damage");
        }

        /** Attacks a second: vanilla's 4 plus what the item adds. */
        double speed() {
            return 4.0 + stats.getOrDefault("attack_speed_flat", 0.0);
        }

        /** The armour budget's points (docs/progression/balance.md section 3): what a piece is worth in one number. */
        double points() {
            return stat("health") + 0.5 * (stat("armor") + stat("magic_resist")) + 4.0 * Math.max(0.0, stat("attack_damage"))
                    + 0.8 * stat("attack_speed") + 1.5 * stat("move_speed") + 1.2 * stat("lethality");
        }
    }

    private static void run() throws IOException {
        Path dir = FMLPaths.GAMEDIR.get();
        List<Row> rows = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getPath().equals("air")) {
                continue;
            }
            Row row = measure(item, id);
            if (row != null) {
                rows.add(row);
            }
        }

        Map<EquipmentSlot, Row> netherite = new EnumMap<>(EquipmentSlot.class);
        for (Row row : rows) {
            if (row.id.getNamespace().equals("minecraft") && row.id.getPath().startsWith("netherite_") && row.category.equals("ARMOR")) {
                netherite.put(row.slot, row);
            }
        }

        StringBuilder csv = new StringBuilder("item,class,category,slot,attack,speed,armor,magic_resist,health,lethality,armor_pen,magic_pen,"
                + "omnivamp,attack_speed_pct,move_speed_pct,vanilla_armor,vanilla_toughness,points,vs_netherite,other,flags\n");
        int noStats = 0;
        int belowNetherite = 0;
        for (Row row : rows) {
            List<String> flags = new ArrayList<>();
            Row ref = row.category.equals("ARMOR") ? netherite.get(row.slot) : null;
            double versus = ref == null || ref.points() == 0.0 ? Double.NaN : row.points() / ref.points();
            if (row.category.equals("ARMOR") && row.points() == 0.0 && row.stat("armor_penetration") == 0 && row.stat("magic_penetration") == 0
                    && row.stat("omnivamp") == 0) {
                flags.add("NO_STATS");
                noStats++;
            }
            if (row.category.equals("RANGED") && row.attack() <= 1.0) {
                flags.add("NO_DAMAGE");
                noStats++;
            }
            if (row.category.equals("WEAPON") && row.attack() <= 1.0) {
                flags.add("NO_DAMAGE");
            }
            if (row.category.equals("ARMOR") && !Double.isNaN(versus) && versus < 1.0 && !isOrdinaryMaterial(row.id)) {
                flags.add("BELOW_NETHERITE");
                belowNetherite++;
            }
            if (row.category.equals("ARMOR") && row.vanillaArmor > 0 && row.stat("armor") == 0) {
                flags.add("UNCONVERTED");
            }
            csv.append(String.join(",", row.id.toString(), row.itemClass, row.category, row.slot.getName(),
                    num(row.attack()), num(row.speed()), num(row.stat("armor")), num(row.stat("magic_resist")), num(row.stat("health")),
                    num(row.stat("lethality")), num(row.stat("armor_penetration")), num(row.stat("magic_penetration")),
                    num(row.stat("omnivamp")), num(row.stat("attack_speed")), num(row.stat("move_speed")),
                    num(row.vanillaArmor), num(row.vanillaToughness), num(row.points()),
                    Double.isNaN(versus) ? "" : String.format(Locale.ROOT, "%.2f", versus),
                    '"' + String.join("; ", row.other) + '"', String.join(" ", flags))).append('\n');
        }
        Files.writeString(dir.resolve("item-audit.csv"), csv.toString(), StandardCharsets.UTF_8);
        log(rows.size() + " weapons, bows, armour and off-hand pieces; " + noStats + " give nothing; " + belowNetherite
                + " armour pieces are below the same slot in netherite");

        auditRecipes(dir, rows);
        listOthers(dir, rows);
    }

    /**
     * Every item from a mod that the audit did not sort into a kind, with what it is, so a weapon or armour piece that carries no
     * attribute modifier at all (and so looks like nothing to the audit) can still be seen by name.
     */
    private static void listOthers(Path dir, List<Row> rows) throws IOException {
        java.util.Set<ResourceLocation> sorted = new java.util.HashSet<>();
        for (Row row : rows) {
            sorted.add(row.id);
        }
        StringBuilder csv = new StringBuilder("item,class,damageable,max_stack,equip_slot,has_modifiers\n");
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("minecraft") || sorted.contains(id) || item instanceof net.minecraft.world.item.BlockItem) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            Equipable equipable = Equipable.get(stack);
            csv.append(String.join(",", id.toString(), item.getClass().getSimpleName(), Boolean.toString(stack.isDamageableItem()),
                    Integer.toString(stack.getMaxStackSize()), equipable == null ? "" : equipable.getEquipmentSlot().getName(),
                    Boolean.toString(!stack.getAttributeModifiers().modifiers().isEmpty()))).append('\n');
            count++;
        }
        Files.writeString(dir.resolve("item-audit-others.csv"), csv.toString(), StandardCharsets.UTF_8);
        log(count + " other items from mods listed");
    }

    /** Leather, chain, iron, gold, turtle and the like are meant to sit under netherite. */
    private static boolean isOrdinaryMaterial(ResourceLocation id) {
        return id.getNamespace().equals("minecraft");
    }

    /** What one item gives, or {@code null} if it is neither a weapon nor armour nor an off-hand piece. */
    private static Row measure(Item item, ResourceLocation id) {
        ItemStack stack = new ItemStack(item);
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

        Equipable equipable = Equipable.get(stack);
        EquipmentSlot natural = equipable == null ? null : equipable.getEquipmentSlot();

        Row best = null;
        for (EquipmentSlot slot : SLOTS) {
            Row row = new Row();
            row.id = id;
            row.itemClass = item.getClass().getSimpleName();
            row.slot = slot;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (!entry.slot().test(slot)) {
                    continue;
                }
                row.any = true;
                add(row, entry);
            }
            boolean better = best == null || (!best.any && row.any)
                    || (row.any && best.any && slot == natural && best.slot != natural)
                    || (!best.any && !row.any && slot == natural);
            if (better) {
                best = row;
            }
        }
        if (best == null) {
            return null;
        }

        boolean armourSlot = best.slot == EquipmentSlot.HEAD || best.slot == EquipmentSlot.CHEST || best.slot == EquipmentSlot.LEGS
                || best.slot == EquipmentSlot.FEET;
        boolean hasStats = best.any && (best.stat("armor") != 0 || best.stat("magic_resist") != 0 || best.stat("health") != 0
                || best.vanillaArmor != 0 || best.vanillaToughness != 0 || best.stat("attack_damage") != 0);
        if (item instanceof ArmorItem || item instanceof ElytraItem || (armourSlot && natural == best.slot && hasStats)) {
            best.category = "ARMOR";
            if (natural != null && natural != best.slot) {
                best = rebuild(item, id, modifiers, natural);
                best.category = "ARMOR";
            }
        } else if (item instanceof ShieldItem || (best.slot == EquipmentSlot.OFFHAND && hasStats)) {
            best.category = "OFFHAND";
        } else if (item instanceof ProjectileWeaponItem) {
            best.category = "RANGED";
        } else if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem) {
            best.category = "WEAPON";
        } else if (item instanceof DiggerItem) {
            best.category = "TOOL";
        } else if (best.any && best.slot == EquipmentSlot.MAINHAND && best.stat("attack_damage") > 0) {
            best.category = "WEAPON";
        } else if (hasStats || best.any && (best.stat("armor") != 0 || best.stat("health") != 0)) {
            best.category = "OTHER";
        } else {
            return null;
        }
        return best;
    }

    private static Row rebuild(Item item, ResourceLocation id, ItemAttributeModifiers modifiers, EquipmentSlot slot) {
        Row row = new Row();
        row.id = id;
        row.itemClass = item.getClass().getSimpleName();
        row.slot = slot;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.slot().test(slot)) {
                row.any = true;
                add(row, entry);
            }
        }
        return row;
    }

    private static void add(Row row, ItemAttributeModifiers.Entry entry) {
        AttributeModifier modifier = entry.modifier();
        if (entry.attribute().is(Attributes.ARMOR)) {
            row.vanillaArmor += modifier.amount();
            return;
        }
        if (entry.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
            row.vanillaToughness += modifier.amount();
            return;
        }
        for (CombatStat stat : CombatStats.all()) {
            if (entry.attribute().value() == stat.attribute().value()) {
                if (stat == CombatStats.ATTACK_SPEED && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    row.stats.merge("attack_speed_flat", modifier.amount(), Double::sum);
                    return;
                }
                row.stats.merge(stat.name(), stat.toWritten(modifier.amount()), Double::sum);
                return;
            }
        }
        row.other.add(BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value()) + " " + modifier.operation().getSerializedName()
                + " " + num(modifier.amount()));
    }

    // ---- recipes: a stronger item turned into a weaker one

    private static void auditRecipes(Path dir, List<Row> rows) throws IOException {
        Map<Item, Row> byItem = new LinkedHashMap<>();
        for (Row row : rows) {
            byItem.put(BuiltInRegistries.ITEM.get(row.id), row);
        }

        StringBuilder csv = new StringBuilder("recipe,type,result,result_score,ingredient,ingredient_score,category,slot\n");
        int flagged = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof SmithingTrimRecipe) {
                continue;
            }
            ItemStack out = recipe.getResultItem(server.registryAccess());
            Row result = out.isEmpty() ? null : byItem.get(out.getItem());
            if (result == null || result.category.equals("TOOL")) {
                continue;
            }
            for (Map.Entry<Item, Row> candidate : byItem.entrySet()) {
                Row ingredient = candidate.getValue();
                if (candidate.getKey() == out.getItem() || !ingredient.category.equals(result.category)
                        || (result.category.equals("ARMOR") && ingredient.slot != result.slot)
                        || !usesItem(recipe, new ItemStack(candidate.getKey()))) {
                    continue;
                }
                double have = score(result);
                double used = score(ingredient);
                if (have < used) {
                    flagged++;
                    csv.append(String.join(",", holder.id().toString(), recipe.getType().toString().replace("minecraft:", ""),
                            result.id.toString(), num(have), ingredient.id.toString(), num(used), result.category, result.slot.getName()))
                            .append('\n');
                }
            }
        }
        Files.writeString(dir.resolve("item-audit-recipes.csv"), csv.toString(), StandardCharsets.UTF_8);
        log(flagged + " recipes make an item out of a stronger one");
    }

    /** One number to compare two items of one kind: armour by its points, a melee weapon by damage a second, a bow by its damage. */
    private static double score(Row row) {
        return switch (row.category) {
            case "ARMOR", "OFFHAND", "OTHER" -> row.points();
            case "RANGED" -> row.attack();
            default -> row.attack() * row.speed();
        };
    }

    private static boolean usesItem(Recipe<?> recipe, ItemStack stack) {
        if (recipe instanceof SmithingRecipe smithing) {
            return smithing.isBaseIngredient(stack) || smithing.isAdditionIngredient(stack);
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static String num(double value) {
        return Math.abs(value - Math.rint(value)) < 1.0e-9 ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
    }

    private static void log(String line) {
        System.out.println("[item-audit] " + line);
    }
}
