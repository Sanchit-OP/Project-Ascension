package com.ascension.combat.internal.enchanting.library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/**
 * The library's screen logic. The enchantments are numbered by the registry sorted by id, which is the same on both sides, so the
 * library's contents and the current selection travel as two small lists of {@link DataSlot}s, and the screen's clicks travel as
 * vanilla's menu-button presses (button {@code i} toggles enchantment {@code i}; button {@code catalog size} takes the combination).
 * No packet of our own.
 *
 * <p>Slots: 0 takes enchanted books and absorbs them into the library at once; 1 holds the blank book the combination is written
 * on; 2 is where the finished book appears. A combination costs experience levels (the sum of its levels), may hold at most
 * {@link #SELECT_MAX} enchantments, and must be compatible with itself.
 */
public final class LibraryMenu extends AbstractContainerMenu {

    public static final int INPUT = 0;
    public static final int BLANK = 1;
    public static final int OUTPUT = 2;

    /** Most enchantments one combination may hold, so a book never exceeds what an item may carry. */
    public static final int SELECT_MAX = 6;

    private final Player player;
    private final ContainerLevelAccess access;
    @Nullable
    private final EnchantmentLibraryBlockEntity library;
    private final Container container = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            LibraryMenu.this.slotsChanged(this);
        }
    };
    private final List<Holder<Enchantment>> catalog;
    private final DataSlot[] stored;
    private final DataSlot[] selected;

    /** Client side: contents arrive through the data slots. */
    public LibraryMenu(int id, Inventory inventory) {
        this(id, inventory, null, ContainerLevelAccess.NULL);
    }

    public LibraryMenu(int id, Inventory inventory, @Nullable EnchantmentLibraryBlockEntity library, ContainerLevelAccess access) {
        super(LibraryContent.MENU.get(), id);
        this.player = inventory.player;
        this.library = library;
        this.access = access;

        List<Holder<Enchantment>> all = new ArrayList<>();
        player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(all::add);
        all.sort(Comparator.comparing(holder -> holder.getKey().location().toString()));
        this.catalog = List.copyOf(all);
        this.stored = new DataSlot[catalog.size()];
        this.selected = new DataSlot[catalog.size()];
        for (int i = 0; i < catalog.size(); i++) {
            stored[i] = addDataSlot(DataSlot.standalone());
            selected[i] = addDataSlot(DataSlot.standalone());
        }

        addSlot(new Slot(container, INPUT, 126, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }
        });
        addSlot(new Slot(container, BLANK, 126, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }
        });
        addSlot(new Slot(container, OUTPUT, 126, 68) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }
    }

    public List<Holder<Enchantment>> catalog() {
        return catalog;
    }

    public int storedLevel(int index) {
        return stored[index].get();
    }

    public boolean isSelected(int index) {
        return selected[index].get() != 0;
    }

    /** Levels the current selection would cost. */
    public int selectedCost() {
        List<Integer> levels = new ArrayList<>();
        for (int i = 0; i < catalog.size(); i++) {
            if (isSelected(i)) {
                levels.add(storedLevel(i));
            }
        }
        return levels.isEmpty() ? 0 : LibraryStore.cost(levels);
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        if (library == null || player.level().isClientSide) {
            return;
        }
        ItemStack book = container.getItem(INPUT);
        if (book.isEmpty()) {
            return;
        }
        ItemEnchantments enchantments = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return;
        }
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            library.store().absorb(holder.getKey().location().toString(), entry.getIntValue(), holder.value().getMaxLevel());
        }
        library.setChanged();
        container.setItem(INPUT, ItemStack.EMPTY);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void broadcastChanges() {
        if (library != null) {
            for (int i = 0; i < catalog.size(); i++) {
                stored[i].set(library.store().level(catalog.get(i).getKey().location().toString()));
                if (stored[i].get() == 0) {
                    selected[i].set(0);
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (library == null) {
            return false;
        }
        if (id >= 0 && id < catalog.size()) {
            return toggle(id);
        }
        if (id == catalog.size()) {
            return take(clicker);
        }
        return false;
    }

    private boolean toggle(int index) {
        if (selected[index].get() != 0) {
            selected[index].set(0);
            return true;
        }
        if (stored[index].get() == 0) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < catalog.size(); i++) {
            if (selected[i].get() != 0) {
                count++;
                if (!Enchantment.areCompatible(catalog.get(i), catalog.get(index))) {
                    return false;
                }
            }
        }
        if (count >= SELECT_MAX) {
            return false;
        }
        selected[index].set(1);
        return true;
    }

    private boolean take(Player clicker) {
        if (!container.getItem(OUTPUT).isEmpty() || !container.getItem(BLANK).is(Items.BOOK)) {
            return false;
        }
        List<Integer> levels = new ArrayList<>();
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        for (int i = 0; i < catalog.size(); i++) {
            if (selected[i].get() != 0 && stored[i].get() > 0) {
                result.enchant(catalog.get(i), stored[i].get());
                levels.add(stored[i].get());
            }
        }
        if (levels.isEmpty()) {
            return false;
        }
        int cost = LibraryStore.cost(levels);
        if (!clicker.getAbilities().instabuild && clicker.experienceLevel < cost) {
            clicker.displayClientMessage(Component.translatable("container.ascension_combat.enchantment_library.too_costly", cost), true);
            return false;
        }
        if (!clicker.getAbilities().instabuild) {
            clicker.giveExperienceLevels(-cost);
        }
        container.removeItem(BLANK, 1);
        container.setItem(OUTPUT, result);
        for (DataSlot slot : selected) {
            slot.set(0);
        }
        clicker.level().playSound(null, clicker.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player mover, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.ENCHANTED_BOOK)) {
            if (!moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.BOOK)) {
            if (!moveItemStackTo(stack, BLANK, BLANK + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player who) {
        return library == null || stillValid(access, who, LibraryContent.BLOCK.get());
    }

    @Override
    public void removed(Player who) {
        super.removed(who);
        access.execute((level, pos) -> clearContainer(who, container));
    }
}
