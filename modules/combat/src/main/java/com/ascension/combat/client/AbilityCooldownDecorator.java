package com.ascension.combat.client;

import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.net.AbilityKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * Draws the same diagonal cooldown swipe vanilla's own {@code ItemCooldowns} paints
 * (docs/technical/combat-api.md section 6) for a bow's own active, reading {@link
 * ClientAbilityCooldowns} instead of {@code LocalPlayer#getCooldowns} &mdash; the maths below is
 * copied by hand from {@code GuiGraphics#renderItemDecorations}'s own vanilla cooldown block, since
 * that method has no public hook to reuse directly.
 */
public final class AbilityCooldownDecorator implements IItemDecorator {

    private final AbilityKey key;

    private AbilityCooldownDecorator(AbilityKey key) {
        this.key = key;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AbilityCooldownDecorator::onRegisterItemDecorations);
    }

    private static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(CombatContent.HEARTWOOD_OF_PALLAS.get(), new AbilityCooldownDecorator(AbilityKey.HEARTWOOD));
        event.register(CombatContent.GALEFORCE.get(), new AbilityCooldownDecorator(AbilityKey.GALEFORCE));
    }

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (Minecraft.getInstance().level == null) {
            return false;
        }

        float fraction = ClientAbilityCooldowns.fractionRemaining(key, Minecraft.getInstance().level.getGameTime());
        if (fraction <= 0.0f) {
            return false;
        }

        int top = yOffset + Mth.floor(16.0f * (1.0f - fraction));
        int bottom = top + Mth.ceil(16.0f * fraction);
        guiGraphics.fill(RenderType.guiOverlay(), xOffset, top, xOffset + 16, bottom, Integer.MAX_VALUE);
        return false;
    }
}
