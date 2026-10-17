package com.ascension.combat.client;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.net.SetRunePagePayload;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageRules;
import com.ascension.combat.internal.rune.RuneRegistries;
import com.ascension.combat.internal.rune.RuneSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rune page screen, <b>interim</b>: plain vanilla buttons, enough to build and save a page
 * of the new shape (docs/technical/rune-system.md section 3) and to test the rules in a client.
 * It is replaced by the orrery (section 10), which Sanchit chose on 2026-10-17; nothing here is
 * meant to survive that but the flow.
 *
 * <p>Reads every rune from the synced rune registry, never a list in code, so what it offers is
 * exactly what the server will accept. Choices are local until Save, which sends one
 * {@link SetRunePagePayload}; the server answers saved or refused, and the reason is shown here
 * ({@link #onResult}) without losing the unsaved choices.
 *
 * <p>Two columns: the primary path on the left, the secondary path and the shards on the right.
 * Buttons are as wide as their names and wrap onto a new line when a column is full, so no name is
 * clipped at any GUI scale.
 */
public final class RuneScreen extends Screen {

    private static final int BUTTON_H = 14;
    private static final int GAP = 2;
    private static final int ROW = BUTTON_H + 4;
    private static final int MARGIN = 8;
    private static final int TOP = 28;

    private RunePath primary;
    private ResourceLocation keystone;
    private final Map<Integer, ResourceLocation> primaryRows = new HashMap<>();
    private RunePath secondary;
    private final Map<Integer, ResourceLocation> secondaryRows = new HashMap<>();
    private final Map<Integer, ResourceLocation> shardRows = new HashMap<>();

    private Component status = Component.empty();

    /** Words drawn behind the buttons, collected while the buttons are laid out. */
    private record Words(Component text, int x, int y, int colour) {
    }

    private final List<Words> words = new ArrayList<>();

    public RuneScreen() {
        super(Component.translatable("screen.ascension_combat.rune_page"));
        resetToSaved();
    }

    private void resetToSaved() {
        RunePage saved = ClientRunePageCache.page();
        primary = saved.primary().orElse(null);
        keystone = saved.keystone().orElse(null);
        secondary = saved.secondary().orElse(null);
        primaryRows.clear();
        secondaryRows.clear();
        shardRows.clear();
        Registry<RuneDefinition> runes = runes();
        if (runes == null) {
            return;
        }
        for (ResourceLocation id : saved.primaryMinors()) {
            put(runes, primaryRows, id);
        }
        for (ResourceLocation id : saved.secondaryMinors()) {
            put(runes, secondaryRows, id);
        }
        for (ResourceLocation id : saved.shards()) {
            put(runes, shardRows, id);
        }
    }

    private static void put(Registry<RuneDefinition> runes, Map<Integer, ResourceLocation> rows, ResourceLocation id) {
        RuneDefinition rune = runes.get(id);
        if (rune != null) {
            rows.put(rune.row(), id);
        }
    }

    private Registry<RuneDefinition> runes() {
        return this.minecraft == null || this.minecraft.level == null
                ? null
                : this.minecraft.level.registryAccess().registryOrThrow(RuneRegistries.RUNE);
    }

    @Override
    protected void init() {
        words.clear();
        Registry<RuneDefinition> runes = runes();
        if (runes == null) {
            return;
        }
        RunePageRules.OpenSlots open = ClientRunePageCache.open();
        int colWidth = this.width / 2 - MARGIN - 4;
        int left = MARGIN;
        int right = this.width / 2 + 4;

        // --- the primary path
        words.add(new Words(Component.translatable("gui.ascension_combat.primary_path"), left, TOP - 10, 0xA0A0A0));
        int y = flow(left, TOP, colWidth, List.of(RunePath.values()), path -> pathLabel(path, path == primary),
                path -> true, path -> {
                    if (path != primary) {
                        primary = path;
                        keystone = null;
                        primaryRows.clear();
                        if (secondary == path) {
                            secondary = null;
                            secondaryRows.clear();
                        }
                    }
                }, path -> null) + 4;
        if (primary == null) {
            words.add(new Words(Component.translatable("gui.ascension_combat.choose_primary"), left, y, 0xFFD87A));
        } else {
            words.add(new Words(Component.translatable("gui.ascension_combat.keystone"), left, y, 0xA0A0A0));
            y += 10;
            List<Entry> keys = of(runes, e -> e.rune.slot() == RuneSlot.KEYSTONE && e.rune.path().orElse(null) == primary);
            y = flow(left, y, colWidth, keys, e -> runeLabel(e, e.id.equals(keystone)), e -> true,
                    e -> keystone = e.id.equals(keystone) ? null : e.id, Entry::tooltip) + 4;
            y = minors(runes, left, y, colWidth, primary, primaryRows);
        }

        // --- the secondary path
        words.add(new Words(Component.translatable("gui.ascension_combat.secondary_path"), right, TOP - 10, 0xA0A0A0));
        y = flow(right, TOP, colWidth, List.of(RunePath.values()), path -> pathLabel(path, path == secondary),
                path -> open.secondary() && path != primary, path -> {
                    secondary = secondary == path ? null : path;
                    secondaryRows.clear();
                }, path -> null) + 4;
        if (!open.secondary()) {
            words.add(new Words(Component.translatable("gui.ascension_combat.secondary_locked"), right, y, 0xFFD87A));
            y += 12;
        } else if (secondary != null) {
            y = minors(runes, right, y, colWidth, secondary, secondaryRows);
        }

        // --- the stat shards
        words.add(new Words(Component.translatable("gui.ascension_combat.rune_shards"), right, y + 4, 0xA0A0A0));
        y += 14;
        boolean anyShard = false;
        for (int r = 0; r < RuneDefinition.SHARD_ROWS; r++) {
            int rowIndex = r;
            boolean rowOpen = open.shardRow(rowIndex);
            List<Entry> shards = of(runes, e -> e.rune.slot() == RuneSlot.SHARD && e.rune.row() == rowIndex);
            anyShard |= !shards.isEmpty();
            y = flow(right, y, colWidth, shards, e -> runeLabel(e, e.id.equals(shardRows.get(rowIndex))), e -> rowOpen,
                    e -> toggle(shardRows, rowIndex, e.id), Entry::tooltip);
        }
        if (!anyShard) {
            words.add(new Words(Component.translatable("gui.ascension_combat.no_shards_yet"), right, y, 0x808080));
        }

        // --- save and reset
        int bottom = this.height - 30;
        RunePage pending = pending();
        boolean costs = RunePageRules.costsToChange(ClientRunePageCache.page(), pending);
        Component save = costs
                ? Component.translatable("gui.ascension_combat.rune_save_cost", ClientRunePageCache.chalk(), ClientRunePageCache.levels())
                : Component.translatable("gui.ascension_combat.rune_save");
        this.addRenderableWidget(Button.builder(save, b -> onSave())
                .bounds(this.width / 2 + 4, bottom, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ascension_combat.rune_reset"), b -> {
            resetToSaved();
            status = Component.empty();
            rebuildWidgets();
        }).bounds(this.width / 2 - 154, bottom, 150, 20).build());
    }

    /** The two minor rows of one path side; returns the y below them. */
    private int minors(Registry<RuneDefinition> runes, int x, int y, int width, RunePath path, Map<Integer, ResourceLocation> rows) {
        words.add(new Words(Component.translatable("gui.ascension_combat.minors"), x, y, 0xA0A0A0));
        y += 10;
        for (int r = 0; r < RuneDefinition.MINOR_ROWS; r++) {
            int rowIndex = r;
            List<Entry> minors = of(runes, e -> e.rune.slot() == RuneSlot.MINOR
                    && e.rune.path().orElse(null) == path && e.rune.row() == rowIndex);
            y = flow(x, y, width, minors, e -> runeLabel(e, e.id.equals(rows.get(rowIndex))), e -> true,
                    e -> toggle(rows, rowIndex, e.id), Entry::tooltip);
        }
        return y;
    }

    /** One rune and its id, for a button. */
    private record Entry(ResourceLocation id, RuneDefinition rune) {
        Tooltip tooltip() {
            return Tooltip.create(Component.translatable(nameKey(id)).append("\n")
                    .append(Component.translatable(nameKey(id) + ".desc")));
        }
    }

    private static List<Entry> of(Registry<RuneDefinition> runes, Predicate<Entry> keep) {
        List<Entry> out = new ArrayList<>();
        for (var e : runes.entrySet()) {
            Entry entry = new Entry(e.getKey().location(), e.getValue());
            if (keep.test(entry)) {
                out.add(entry);
            }
        }
        out.sort(Comparator.comparingInt((Entry e) -> e.rune.position()).thenComparing(e -> e.id.toString()));
        return out;
    }

    /**
     * Lays buttons out left to right, each as wide as its name, wrapping at the column's edge.
     * Returns the y below the last line (the same y when there is nothing to lay out).
     */
    private <T> int flow(int x, int y, int width, List<T> items, Function<T, Component> label,
            Predicate<T> active, Consumer<T> onClick, Function<T, Tooltip> tooltip) {
        if (items.isEmpty()) {
            return y;
        }
        int cx = x;
        for (T item : items) {
            Component text = label.apply(item);
            int w = this.font.width(text) + 12;
            if (cx > x && cx + w > x + width) {
                cx = x;
                y += ROW;
            }
            Button button = Button.builder(text, b -> {
                        onClick.accept(item);
                        status = Component.empty();
                        rebuildWidgets();
                    })
                    .bounds(cx, y, w, BUTTON_H)
                    .tooltip(tooltip.apply(item))
                    .build();
            button.active = active.test(item);
            this.addRenderableWidget(button);
            cx += w + GAP;
        }
        return y + ROW;
    }

    private static void toggle(Map<Integer, ResourceLocation> rows, int row, ResourceLocation id) {
        if (id.equals(rows.get(row))) {
            rows.remove(row);
        } else {
            rows.put(row, id);
        }
    }

    private RunePage pending() {
        return new RunePage(
                Optional.ofNullable(primary),
                Optional.ofNullable(keystone),
                List.copyOf(primaryRows.values()),
                Optional.ofNullable(secondary),
                List.copyOf(secondaryRows.values()),
                List.copyOf(shardRows.values()));
    }

    private void onSave() {
        PacketDistributor.sendToServer(new SetRunePagePayload(pending()));
    }

    /** The server's answer to a save. */
    public void onResult(Optional<RunePageRules.Refusal> refusal) {
        status = refusal
                .<Component>map(why -> Component.translatable(why.langKey()))
                .orElse(Component.translatable("gui.ascension_combat.rune_saved"));
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        for (Words w : words) {
            graphics.drawString(this.font, w.text, w.x, w.y, w.colour);
        }
        graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 44, 0xFFD87A);
    }

    private static String nameKey(ResourceLocation id) {
        return "rune." + id.getNamespace() + "." + id.getPath();
    }

    private static Component pathLabel(RunePath path, boolean selected) {
        Component name = Component.translatable("rune_path.ascension_combat." + path.getSerializedName());
        return selected ? Component.translatable("gui.ascension_combat.selected", name) : name;
    }

    private static Component runeLabel(Entry entry, boolean selected) {
        Component name = Component.translatable(nameKey(entry.id));
        return selected ? Component.translatable("gui.ascension_combat.selected", name) : name;
    }
}
