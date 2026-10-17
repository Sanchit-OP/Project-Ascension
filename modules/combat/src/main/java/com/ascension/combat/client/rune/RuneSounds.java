package com.ascension.combat.client.rune;

import com.ascension.combat.api.rune.RunePath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The rune screen's sounds (docs/technical/rune-system.md section 10): all vanilla events, played
 * as interface sounds, so no sound file is added. A quiet tick on hover, a chime on choosing that
 * is pitched by path, a heavier bell for a keystone, a dull note when refused, a seal on Save.
 */
public final class RuneSounds {

    private RuneSounds() {
    }

    private static float pitch(RunePath path) {
        return switch (path) {
            case VANGUARD -> 1.0f;
            case REAVER -> 0.75f;
            case AETHERWRIGHT -> 1.5f;
            case BULWARK -> 0.6f;
        };
    }

    public static void hover() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9f, 0.12f));
    }

    /** Choosing a rune or a path: a chime in that path's pitch; a keystone rings a bell under it. */
    public static void choose(RunePath path, boolean keystone) {
        var sounds = Minecraft.getInstance().getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch(path), 0.8f));
        if (keystone) {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.BELL_BLOCK, pitch(path) * 0.8f, 0.35f));
        }
    }

    public static void refused() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 1.0f));
    }

    public static void saved() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.7f));
    }
}
