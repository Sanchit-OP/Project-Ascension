package com.ascension.combat.client;

import com.ascension.combat.internal.enchanting.library.LibraryMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Enchantment Library's screen: the stored enchantments as a list on the left (click one to pick it for the combination;
 * scroll for more), the book slots on the right, and a button that takes the picked combination onto the blank book. Drawn with
 * flat fills only, so it needs no texture. Clicks are vanilla menu-button presses (see {@link LibraryMenu}).
 */
public final class LibraryScreen extends AbstractContainerScreen<LibraryMenu> {

    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 112;
    private static final int ROW_H = 10;
    private static final int ROWS = 11;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int WELL = 0xFF8B8B8B;
    private static final int PICKED = 0xFF7B5BB8;

    private int scroll;
    private Button take;

    public LibraryScreen(LibraryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        take = addRenderableWidget(Button.builder(Component.translatable("container.ascension_combat.enchantment_library.take"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.catalog().size()))
                .bounds(leftPos + 124, topPos + 92, 44, 14).build());
    }

    /** Indexes (into the catalog) of the enchantments the library holds, in the menu's order. */
    private List<Integer> held() {
        List<Integer> held = new ArrayList<>();
        for (int i = 0; i < menu.catalog().size(); i++) {
            if (menu.storedLevel(i) > 0) {
                held.add(i);
            }
        }
        return held;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 1, LIGHT);
        graphics.fill(x, y, x + 1, y + imageHeight, LIGHT);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + ROWS * ROW_H, DARK);

        List<Integer> held = held();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, held.size() - ROWS)));
        for (int row = 0; row < ROWS && scroll + row < held.size(); row++) {
            int index = held.get(scroll + row);
            int rowY = y + LIST_Y + row * ROW_H;
            if (menu.isSelected(index)) {
                graphics.fill(x + LIST_X + 1, rowY, x + LIST_X + LIST_W - 1, rowY + ROW_H, PICKED);
            }
            Component name = Enchantment.getFullname(menu.catalog().get(index), menu.storedLevel(index));
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), LIST_W - 6), x + LIST_X + 3, rowY + 1, 0xFFFFFF, false);
        }
        if (held.isEmpty()) {
            graphics.drawString(font, Component.translatable("container.ascension_combat.enchantment_library.empty"),
                    x + LIST_X + 3, y + LIST_Y + 3, 0xA0A0A0, false);
        }

        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, WELL);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, DARK);
        }
        graphics.drawString(font, Component.translatable("container.ascension_combat.enchantment_library.in"), x + 152, y + 24, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.ascension_combat.enchantment_library.blank"), x + 152, y + 48, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.ascension_combat.enchantment_library.out"), x + 152, y + 72, 0x404040, false);

        int cost = menu.selectedCost();
        graphics.drawString(font, cost == 0 ? Component.empty()
                        : Component.translatable("container.ascension_combat.enchantment_library.cost", cost),
                x + 124, y + 110, 0x404040, false);
        take.active = cost > 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relX = mouseX - leftPos - LIST_X;
        double relY = mouseY - topPos - LIST_Y;
        if (relX >= 0 && relX < LIST_W && relY >= 0 && relY < ROWS * ROW_H) {
            List<Integer> held = held();
            int row = scroll + (int) (relY / ROW_H);
            if (row < held.size()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, held.get(row));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
