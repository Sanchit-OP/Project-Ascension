package com.ascension.combat.client;

import com.ascension.combat.internal.CombatAttributes;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Read-only stat display (docs/technical/combat-api.md section 3) for the local player &mdash;
 * this module's second {@code Screen}, and its first that reads live game state rather than
 * editing saved state. Needs no network round-trip at all: every stat shown is already {@code
 * setSyncable(true)}, so the client's own copy of {@link Minecraft#player} already carries the
 * same numbers the server does.
 *
 * <p>Built the same day Lifesteal/Omnivamp and Armor Pen/Magic Pen were promoted from scattered
 * per-item constants into real attributes (see {@link CombatAttributes}) &mdash; this screen is
 * the reason that promotion mattered: before it, "your current Omnivamp" was not a number that
 * existed anywhere to read, let alone display.
 *
 * <p>Armor Pen is shown as one combined line, Lethality then Armor Pen% ("23/35%"), matching the
 * order the defense formula itself applies them in &mdash; percent first, then flat off whatever's
 * left ({@link com.ascension.combat.internal.mechanics.PhysicalDamage}). Magic Pen gets its own
 * separate line, not folded into the same row &mdash; briefly unified as one universal
 * "Penetration" stat, split back apart the same day Sanchit asked for the two to read as genuinely
 * different numbers on this screen, the same way League itself keeps them.
 */
public final class CharacterSheetScreen extends Screen {

    private static final int LINE_HEIGHT = 14;
    private static final int SECTION_GAP = 6;
    private static final int START_Y = 34;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int VALUE_COLOR = 0xFFFFFF;

    public CharacterSheetScreen() {
        super(Component.translatable("screen.ascension_combat.character_sheet"));
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

        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_health",
                formatCurrentMax(player.getHealth(), player.getMaxHealth()));
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_attack_damage",
                formatFlat(player, Attributes.ATTACK_DAMAGE));
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_attack_speed",
                formatFlat(player, Attributes.ATTACK_SPEED));
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_move_speed",
                formatFlat(player, Attributes.MOVEMENT_SPEED));

        y += SECTION_GAP;
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_armor",
                formatFlat(player, CombatAttributes.ARMOR));
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_magic_resist",
                formatFlat(player, CombatAttributes.MAGIC_RESIST));

        y += SECTION_GAP;
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_omnivamp",
                formatPercent(player, CombatAttributes.OMNIVAMP));
        y = statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_armor_penetration",
                formatArmorPenetration(player));
        statRow(guiGraphics, labelX, valueX, y, "gui.ascension_combat.stat_magic_penetration",
                formatPercent(player, CombatAttributes.MAGIC_PENETRATION_PERCENT));
    }

    private int statRow(GuiGraphics guiGraphics, int labelX, int valueX, int y, String labelKey, String value) {
        guiGraphics.drawString(this.font, Component.translatable(labelKey), labelX, y, LABEL_COLOR, false);
        guiGraphics.drawString(this.font, value, valueX, y, VALUE_COLOR, false);
        return y + LINE_HEIGHT;
    }

    private static String formatCurrentMax(float current, float max) {
        return Math.round(current) + " / " + Math.round(max);
    }

    private static String formatFlat(Player player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? "0" : String.format(Locale.ROOT, "%.1f", instance.getValue());
    }

    private static String formatPercent(Player player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        double value = instance == null ? 0.0 : instance.getValue();
        return Math.round(value * 100.0) + "%";
    }

    private static String formatArmorPenetration(Player player) {
        AttributeInstance lethality = player.getAttribute(CombatAttributes.LETHALITY);
        AttributeInstance penetrationPercent = player.getAttribute(CombatAttributes.ARMOR_PENETRATION_PERCENT);
        long lethalityValue = lethality == null ? 0L : Math.round(lethality.getValue());
        long percentValue = penetrationPercent == null ? 0L : Math.round(penetrationPercent.getValue() * 100.0);
        return lethalityValue + " / " + percentValue + "%";
    }
}
