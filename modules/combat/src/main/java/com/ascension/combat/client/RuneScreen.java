package com.ascension.combat.client;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.net.SetRunePagePayload;
import com.ascension.combat.internal.rune.RuneCatalog;
import com.ascension.combat.internal.rune.RuneDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rune-page selection screen (docs/technical/combat-api.md section 7) &mdash; this module's
 * first {@code Screen}. Entirely vanilla {@link Button} widgets rather than a custom-textured
 * panel: the same "borrow what already exists rather than block on art" placeholder reasoning
 * every item in {@code CombatContent} uses for its own model, applied to UI instead of an item
 * texture. A stylized panel mockup exists as the actual visual target; this is the functional
 * placeholder, same relationship every item has between its catalog identity and its current
 * borrowed vanilla texture.
 *
 * <p>Deliberately not 1:1 with League's own rune page layout, per Sanchit &mdash; primary path row,
 * an auto-granted keystone, up to two minors, a secondary path row with its own up-to-two minors,
 * confirm. Same two-tree structure the design doc calls for (section 7), different presentation.
 *
 * <p>Rebuilds its whole widget set on every click ({@link #refresh()}, wrapping the framework's own
 * {@link Screen#rebuildWidgets()}) rather than relabeling individual buttons in place &mdash; simpler
 * code for a menu with under twenty widgets total, and ADR-0007's per-tick performance rules have
 * nothing to say about a screen that only redraws on a player click.
 *
 * <p>All selection state here is local until {@link #onConfirm} sends {@link SetRunePagePayload} &mdash;
 * nothing is saved server-side by merely opening this screen or clicking around in it.
 */
public final class RuneScreen extends Screen {

    private static final int PATH_BUTTON_WIDTH = 100;
    private static final int MINOR_BUTTON_WIDTH = 124;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 8;

    private static final int Y_PRIMARY_LABEL = 26;
    private static final int Y_PRIMARY_PATH_ROW = 38;
    private static final int Y_KEYSTONE_LABEL = 64;
    private static final int Y_PRIMARY_MINORS_LABEL = 80;
    private static final int Y_PRIMARY_MINORS_ROW = 92;
    private static final int Y_SECONDARY_LABEL = 120;
    private static final int Y_SECONDARY_PATH_ROW = 132;
    private static final int Y_SECONDARY_MINORS_LABEL = 158;
    private static final int Y_SECONDARY_MINORS_ROW = 170;
    private static final int Y_CONFIRM = 200;

    private RunePath pendingPrimaryPath;
    private final Set<ResourceLocation> pendingPrimaryMinors;
    private RunePath pendingSecondaryPath;
    private final Set<ResourceLocation> pendingSecondaryMinors;

    public RuneScreen() {
        super(Component.translatable("screen.ascension_combat.rune_page"));
        this.pendingPrimaryPath = ClientRunePageCache.primaryPath();
        this.pendingPrimaryMinors = new HashSet<>(ClientRunePageCache.primaryMinors());
        this.pendingSecondaryPath = ClientRunePageCache.secondaryPath();
        this.pendingSecondaryMinors = new HashSet<>(ClientRunePageCache.secondaryMinors());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        addRow(centerX, Y_PRIMARY_PATH_ROW, RunePath.values(), PATH_BUTTON_WIDTH, path ->
                pathLabel(path, path == pendingPrimaryPath, path == pendingSecondaryPath), path -> onPrimaryPathClicked(path));

        if (pendingPrimaryPath != null) {
            List<RuneDefinition> primaryMinors = RuneCatalog.minorsOf(pendingPrimaryPath);
            addRow(centerX, Y_PRIMARY_MINORS_ROW, primaryMinors.toArray(new RuneDefinition[0]), MINOR_BUTTON_WIDTH, rune ->
                    minorLabel(rune, pendingPrimaryMinors.contains(rune.id())), rune -> onMinorClicked(pendingPrimaryMinors, rune.id()));
        }

        addRow(centerX, Y_SECONDARY_PATH_ROW, RunePath.values(), PATH_BUTTON_WIDTH, path ->
                pathLabel(path, path == pendingSecondaryPath, path == pendingPrimaryPath), path -> onSecondaryPathClicked(path));

        if (pendingSecondaryPath != null) {
            List<RuneDefinition> secondaryMinors = RuneCatalog.minorsOf(pendingSecondaryPath);
            addRow(centerX, Y_SECONDARY_MINORS_ROW, secondaryMinors.toArray(new RuneDefinition[0]), MINOR_BUTTON_WIDTH, rune ->
                    minorLabel(rune, pendingSecondaryMinors.contains(rune.id())), rune -> onMinorClicked(pendingSecondaryMinors, rune.id()));
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ascension_combat.confirm"), btn -> onConfirm())
                .bounds(centerX - 50, Y_CONFIRM, 100, BUTTON_HEIGHT)
                .build());
    }

    /** One row of equal-width buttons, centered under {@code centerX}, one item per button. */
    private <T> void addRow(
            int centerX,
            int y,
            T[] items,
            int buttonWidth,
            java.util.function.Function<T, Component> label,
            java.util.function.Consumer<T> onClick) {
        if (items.length == 0) {
            return;
        }

        int totalWidth = items.length * buttonWidth + (items.length - 1) * GAP;
        int startX = centerX - totalWidth / 2;
        for (int i = 0; i < items.length; i++) {
            T item = items[i];
            int x = startX + i * (buttonWidth + GAP);
            this.addRenderableWidget(Button.builder(label.apply(item), btn -> onClick.accept(item))
                    .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void onPrimaryPathClicked(RunePath path) {
        if (path == pendingSecondaryPath || path == pendingPrimaryPath) {
            return;
        }
        this.pendingPrimaryPath = path;
        this.pendingPrimaryMinors.clear();
        refresh();
    }

    private void onSecondaryPathClicked(RunePath path) {
        if (path == pendingPrimaryPath) {
            return;
        }
        this.pendingSecondaryPath = pendingSecondaryPath == path ? null : path;
        this.pendingSecondaryMinors.clear();
        refresh();
    }

    private void onMinorClicked(Set<ResourceLocation> minors, ResourceLocation id) {
        if (minors.contains(id)) {
            minors.remove(id);
        } else if (minors.size() < 2) {
            minors.add(id);
        }
        refresh();
    }

    private void onConfirm() {
        if (pendingPrimaryPath == null) {
            return;
        }

        List<ResourceLocation> primaryMinors = List.copyOf(pendingPrimaryMinors);
        List<ResourceLocation> secondaryMinors = List.copyOf(pendingSecondaryMinors);
        PacketDistributor.sendToServer(
                new SetRunePagePayload(pendingPrimaryPath, primaryMinors, pendingSecondaryPath, secondaryMinors));
        ClientRunePageCache.update(pendingPrimaryPath, pendingPrimaryMinors, pendingSecondaryPath, pendingSecondaryMinors);
        this.onClose();
    }

    private void refresh() {
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font, Component.translatable("gui.ascension_combat.primary_path"), centerX, Y_PRIMARY_LABEL, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, keystoneLabel(), centerX, Y_KEYSTONE_LABEL, 0xFFD87A);
        guiGraphics.drawCenteredString(this.font, minorsLabel(pendingPrimaryPath), centerX, Y_PRIMARY_MINORS_LABEL, 0xA0A0A0);
        guiGraphics.drawCenteredString(
                this.font, Component.translatable("gui.ascension_combat.secondary_path"), centerX, Y_SECONDARY_LABEL, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, minorsLabel(pendingSecondaryPath), centerX, Y_SECONDARY_MINORS_LABEL, 0xA0A0A0);
    }

    private Component keystoneLabel() {
        if (pendingPrimaryPath == null) {
            return Component.translatable("gui.ascension_combat.choose_primary_path");
        }
        return RuneCatalog.keystoneOf(pendingPrimaryPath)
                .<Component>map(rune -> Component.translatable("gui.ascension_combat.keystone", rune.displayName()))
                .orElse(Component.translatable("gui.ascension_combat.no_runes_yet"));
    }

    private Component minorsLabel(RunePath path) {
        if (path == null) {
            return Component.empty();
        }
        if (RuneCatalog.minorsOf(path).isEmpty()) {
            return Component.translatable("gui.ascension_combat.no_runes_yet");
        }
        return Component.translatable("gui.ascension_combat.minor_runes");
    }

    private static Component pathLabel(RunePath path, boolean selected, boolean disabled) {
        String key = "rune_path.ascension_combat." + path.name().toLowerCase(Locale.ROOT);
        Component name = Component.translatable(key);
        if (disabled) {
            return Component.translatable("gui.ascension_combat.path_taken", name);
        }
        return selected ? Component.translatable("gui.ascension_combat.selected", name) : name;
    }

    private static Component minorLabel(RuneDefinition rune, boolean selected) {
        return selected
                ? Component.translatable("gui.ascension_combat.selected", rune.displayName())
                : rune.displayName();
    }
}
