package com.ascension.combat.client;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatBreakdown;
import com.ascension.combat.api.stat.StatReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Read-only stat display (docs/technical/combat-api.md section 3) for the local player &mdash;
 * this module's second {@code Screen}, and its first that reads live game state rather than
 * editing saved state. Needs no network round-trip at all: every stat shown is synced, so the
 * client's own copy of {@link Minecraft#player} already carries the same numbers the server does.
 *
 * <p>Built the same day Lifesteal/Omnivamp and Armor Pen/Magic Pen were promoted from scattered
 * per-item constants into real attributes &mdash; this screen is the reason that promotion
 * mattered: before it, "your current Omnivamp" was not a number that existed anywhere to read, let
 * alone display.
 *
 * <p>Armor Pen is shown as one combined line, Lethality then Armor Pen% ("23/35%"), matching the
 * order the defense formula itself applies them in &mdash; percent first, then flat off whatever's
 * left ({@link com.ascension.combat.internal.mechanics.PhysicalDamage}). Magic Pen gets its own
 * separate line, not folded into the same row &mdash; briefly unified as one universal
 * "Penetration" stat, split back apart the same day Sanchit asked for the two to read as genuinely
 * different numbers on this screen, the same way League itself keeps them.
 *
 * <p><b>Sources, added 2026-10-16</b> (docs/technical/item-stats.md section 7). Every number is read
 * through {@link StatReader}, and hovering a row lists where it comes from: the base value, each
 * worn or held item by name, then growth, runes, effects and anything else. Attack Damage used to
 * read 1 here whatever was held, because vanilla never synced it; it is synced now.
 */
public final class CharacterSheetScreen extends Screen {

    private static final int LINE_HEIGHT = 14;
    private static final int SECTION_GAP = 6;
    private static final int START_Y = 34;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int VALUE_COLOR = 0xFFFFFF;
    private static final int HINT_COLOR = 0x707070;

    /** One line on the sheet: a label, how its value reads, and the stats its hover breaks down. */
    private record Row(String labelKey, Function<Player, String> value, List<CombatStat> stats, boolean gapBefore) {
    }

    private static final List<Row> ROWS = List.of(
            new Row("gui.ascension_combat.stat_health",
                    player -> Math.round(player.getHealth()) + " / " + Math.round(player.getMaxHealth()),
                    List.of(CombatStats.HEALTH), false),
            single("gui.ascension_combat.stat_attack_damage", CombatStats.ATTACK_DAMAGE, false),
            single("gui.ascension_combat.stat_attack_speed", CombatStats.ATTACK_SPEED, false),
            single("gui.ascension_combat.stat_move_speed", CombatStats.MOVE_SPEED, false),
            single("gui.ascension_combat.stat_armor", CombatStats.ARMOR, true),
            single("gui.ascension_combat.stat_magic_resist", CombatStats.MAGIC_RESIST, false),
            single("gui.ascension_combat.stat_omnivamp", CombatStats.OMNIVAMP, true),
            new Row("gui.ascension_combat.stat_armor_penetration",
                    player -> format(CombatStats.LETHALITY, StatReader.value(player, CombatStats.LETHALITY))
                            + " / " + format(CombatStats.ARMOR_PENETRATION, StatReader.value(player, CombatStats.ARMOR_PENETRATION)),
                    List.of(CombatStats.LETHALITY, CombatStats.ARMOR_PENETRATION), false),
            single("gui.ascension_combat.stat_magic_penetration", CombatStats.MAGIC_PENETRATION, false));

    public CharacterSheetScreen() {
        super(Component.translatable("screen.ascension_combat.character_sheet"));
    }

    private static Row single(String labelKey, CombatStat stat, boolean gapBefore) {
        return new Row(labelKey, player -> format(stat, StatReader.value(player, stat)), List.of(stat), gapBefore);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ascension_combat.close"), btn -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, VALUE_COLOR);

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int labelX = centerX - 110;
        int valueX = centerX + 60;
        int y = START_Y;
        Row hovered = null;

        for (Row row : ROWS) {
            if (row.gapBefore()) {
                y += SECTION_GAP;
            }

            guiGraphics.drawString(this.font, Component.translatable(row.labelKey()), labelX, y, LABEL_COLOR, false);
            guiGraphics.drawString(this.font, row.value().apply(player), valueX, y, VALUE_COLOR, false);
            if (mouseX >= labelX && mouseX < valueX + 60 && mouseY >= y - 2 && mouseY < y + LINE_HEIGHT - 2) {
                hovered = row;
            }
            y += LINE_HEIGHT;
        }

        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.ascension_combat.stat_sources_hint"),
                centerX, this.height - 44, HINT_COLOR);

        if (hovered != null) {
            guiGraphics.renderComponentTooltip(this.font, sourceLines(player, hovered), mouseX, mouseY);
        }
    }

    /** The hover card: for each stat in the row, its total, then each source's share. */
    private static List<Component> sourceLines(Player player, Row row) {
        List<Component> lines = new ArrayList<>();
        for (CombatStat stat : row.stats()) {
            if (!lines.isEmpty()) {
                lines.add(Component.empty());
            }

            StatBreakdown breakdown = StatReader.breakdown(player, stat);
            lines.add(Component.translatable(stat.attribute().value().getDescriptionId())
                    .append(" " + format(stat, breakdown.total()))
                    .withStyle(ChatFormatting.WHITE));
            boolean first = true;
            for (StatBreakdown.Part part : breakdown.parts()) {
                String amount = first ? format(stat, part.amount()) : signed(stat, part.amount());
                lines.add(Component.literal("  ").append(part.source()).append(": " + amount)
                        .withStyle(ChatFormatting.GRAY));
                first = false;
            }
        }
        return lines;
    }

    /** A stat's value as the sheet shows it: percentages for the fraction stats, trimmed decimals otherwise. */
    private static String format(CombatStat stat, double value) {
        if (stat == CombatStats.OMNIVAMP || stat == CombatStats.ARMOR_PENETRATION || stat == CombatStats.MAGIC_PENETRATION) {
            return trimmed(value * 100.0, 1) + "%";
        }
        if (stat == CombatStats.ATTACK_SPEED) {
            return trimmed(value, 2);
        }
        if (stat == CombatStats.MOVE_SPEED) {
            return trimmed(value, 3);
        }
        return trimmed(value, 1);
    }

    private static String signed(CombatStat stat, double value) {
        String formatted = format(stat, value);
        return value > 0.0 ? "+" + formatted : formatted;
    }

    private static String trimmed(double value, int decimals) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0).toPlainString() : rounded.toPlainString();
    }
}
