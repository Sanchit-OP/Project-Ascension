package com.ascension.combat.client.hud;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import com.ascension.core.api.client.HudIcon;
import com.ascension.core.api.client.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * League's stat block, slimmed: two columns of three, an icon and a number each, in a glass panel
 * just left of the hotbar. Nothing else on it: the sources of each number live on the character
 * sheet and in item tooltips.
 *
 * <p>Registered with the HUD through {@code core}'s contract, so combat never depends on the HUD
 * module; with no HUD host installed it is simply never drawn (ADR-0022). Reads every stat through
 * {@link StatReader} (ADR-0019). A number is green for three seconds after a buff lifts it and red
 * after a debuff cuts it.
 *
 * <p>Runs every frame (ADR-0007): the text is rebuilt only when a value changes, and the player is
 * read each frame, never stored.
 */
@OnlyIn(Dist.CLIENT)
public final class StatBlockElement implements HudElement {

    private static final int COLUMN_WIDTH = 33;
    private static final int WIDTH = COLUMN_WIDTH * 2;
    private static final int ROW_HEIGHT = 10;
    private static final int HEIGHT = ROW_HEIGHT * 3 + 6;
    private static final int ICON = 8;

    private static final int TEXT = 0xF4F6FA;
    private static final int UP = 0x7AF08A;
    private static final int DOWN = 0xFF7A70;

    private enum Format { POINTS, ATTACK_SPEED, PERCENT, MOVE_SPEED }

    private record Cell(CombatStat stat, HudIcon icon, Format format) {
    }

    /** Cells in drawing order: left column top to bottom, then the right. */
    private static final Cell[] CELLS = {
            new Cell(CombatStats.ATTACK_DAMAGE, HudIcon.ATTACK_DAMAGE, Format.POINTS),
            new Cell(CombatStats.ATTACK_SPEED, HudIcon.ATTACK_SPEED, Format.ATTACK_SPEED),
            new Cell(CombatStats.OMNIVAMP, HudIcon.OMNIVAMP, Format.PERCENT),
            new Cell(CombatStats.ARMOR, HudIcon.ARMOR, Format.POINTS),
            new Cell(CombatStats.MAGIC_RESIST, HudIcon.MAGIC_RESIST, Format.POINTS),
            new Cell(CombatStats.MOVE_SPEED, HudIcon.MOVE_SPEED, Format.MOVE_SPEED),
    };

    private final StatTrend[] trends = new StatTrend[CELLS.length];
    private final double[] shownValue = new double[CELLS.length];
    private final Component[] text = new Component[CELLS.length];

    public StatBlockElement() {
        for (int i = 0; i < CELLS.length; i++) {
            trends[i] = new StatTrend();
            shownValue[i] = Double.NaN;
        }
    }

    /** Forget colour history and cached text, on leaving a world. */
    public void reset() {
        for (int i = 0; i < CELLS.length; i++) {
            trends[i].reset();
            shownValue[i] = Double.NaN;
            text[i] = null;
        }
    }

    @Override
    public HudAnchor anchor() {
        return HudAnchor.HOTBAR_LEFT;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public boolean visible() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isSpectator();
    }

    @Override
    public void render(GuiGraphics g, HudTheme theme, float partialTick, float opacity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Font font = mc.font;
        long tick = player.level().getGameTime();

        theme.panel(g, 0, 0, WIDTH, HEIGHT, opacity);
        theme.divider(g, COLUMN_WIDTH, 5, HEIGHT - 10, opacity);

        int alpha = Math.max(4, Math.min(255, Math.round(opacity * 255.0f)));
        for (int i = 0; i < CELLS.length; i++) {
            Cell cell = CELLS[i];
            double value = StatReader.value(player, cell.stat());
            trends[i].update(value, tick);
            if (text[i] == null || value != shownValue[i]) {
                shownValue[i] = value;
                text[i] = theme.bold(format(cell.format(), value));
            }

            int cellX = (i / 3) * COLUMN_WIDTH;
            int y = 4 + (i % 3) * ROW_HEIGHT;

            theme.icon(g, cell.icon(), cellX + 4, y, ICON, cell.stat().color(), opacity);

            int direction = trends[i].direction(tick);
            int colour = direction > 0 ? UP : direction < 0 ? DOWN : TEXT;
            int right = cellX + COLUMN_WIDTH - 4;
            g.drawString(font, text[i], right - font.width(text[i]), y + (ICON - font.lineHeight) / 2 + 1,
                    (alpha << 24) | colour, true);
        }
    }

    private static String format(Format format, double value) {
        return switch (format) {
            case POINTS -> StatFormat.points(value);
            case ATTACK_SPEED -> StatFormat.attackSpeed(value);
            case PERCENT -> StatFormat.percent(value);
            case MOVE_SPEED -> StatFormat.moveSpeed(value);
        };
    }
}
