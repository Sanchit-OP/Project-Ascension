package com.ascension.combat.internal.rune;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * What changing a rune costs (docs/technical/rune-system.md section 7, Sanchit 2026-10-15): Rune
 * Chalk and experience levels, both, paid once per save that removes or replaces a rune. Creative
 * players pay nothing.
 */
public final class RuneChangeCost {

    private RuneChangeCost() {
    }

    public static int chalk() {
        return RuneConfig.INSTANCE.changeChalk.get();
    }

    public static int levels() {
        return RuneConfig.INSTANCE.changeLevels.get();
    }

    /** Why this player can't pay, or nothing if they can. */
    public static Optional<RunePageRules.Refusal> check(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return Optional.empty();
        }
        if (countChalk(player.getInventory()) < chalk()) {
            return Optional.of(RunePageRules.Refusal.NO_CHALK);
        }
        if (player.experienceLevel < levels()) {
            return Optional.of(RunePageRules.Refusal.NO_LEVELS);
        }
        return Optional.empty();
    }

    /** Takes the chalk and the levels. Call only after {@link #check} passed. */
    public static void pay(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        int owed = chalk();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && owed > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RuneContent.RUNE_CHALK.get())) {
                int take = Math.min(owed, stack.getCount());
                stack.shrink(take);
                owed -= take;
            }
        }
        player.giveExperienceLevels(-levels());
    }

    private static int countChalk(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RuneContent.RUNE_CHALK.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
