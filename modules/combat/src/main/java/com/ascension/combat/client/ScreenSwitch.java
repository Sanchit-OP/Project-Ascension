package com.ascension.combat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Lets the three player screens lead to each other: while one is open, the other two's keys open
 * them (rune page, skill tree, character sheet), so a player never has to close one to reach the next.
 *
 * <p>Each screen asks here from its own {@code keyPressed}, after checking its own key (which
 * closes it). Switching is the same as closing and opening: anything marked in the old screen and
 * not yet saved or confirmed is left behind, exactly as it is when the key that opened it is pressed
 * again.
 */
final class ScreenSwitch {

    private ScreenSwitch() {
    }

    /** True if the key opened another screen in place of {@code current}. */
    static boolean handle(Screen current, int keyCode, int scanCode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(current instanceof RuneScreen) && CombatKeybinds.isOpenRunePageKey(keyCode, scanCode)) {
            minecraft.setScreen(new RuneScreen());
            return true;
        }
        if (!(current instanceof GrowthScreen) && GrowthKeybind.OPEN.matches(keyCode, scanCode)) {
            minecraft.setScreen(new GrowthScreen());
            return true;
        }
        if (!(current instanceof CharacterSheetScreen) && CombatKeybinds.isOpenCharacterSheetKey(keyCode, scanCode)) {
            minecraft.setScreen(new CharacterSheetScreen());
            return true;
        }
        return false;
    }
}
