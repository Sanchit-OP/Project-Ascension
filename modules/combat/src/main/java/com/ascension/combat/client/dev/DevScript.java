package com.ascension.combat.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The shared spine of the development harnesses ({@link RuneScreenshots}, {@link RuneEffectChecks}):
 * a list of steps, each an action and a number of client ticks to wait after it, that opens the
 * singleplayer test world by itself once the game has settled on a menu, then runs the steps one
 * after another. Dormant unless the harness's trigger file sits in the game directory.
 */
final class DevScript {

    /** The singleplayer save the client opens by itself: a folder named this in the game directory's saves. */
    private static final String WORLD = "runes-test";

    private record Step(int waitAfter, Runnable action) {
    }

    private final String tag;
    private final List<Step> steps = new ArrayList<>();
    private int next;
    private int wait;
    private int menuTicks;
    private boolean opening;

    DevScript(String tag) {
        this.tag = tag;
    }

    /** Whether the trigger file {@code name} is in the game directory; it is deleted, so it arms one run. */
    static boolean armed(String name) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(name);
        if (!Files.exists(trigger)) {
            return false;
        }

        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /** After leaving the world: let the script open it again, once the menu has settled. */
    void reopenWorld() {
        this.opening = false;
        this.menuTicks = 0;
    }

    void add(int waitAfter, Runnable action) {
        this.steps.add(new Step(waitAfter, action));
    }

    void log(String message) {
        System.out.println("[" + this.tag + "] " + message);
    }

    /** Call every client tick: opens the world, then runs the next step when the wait is over. */
    void tick(Minecraft mc) {
        if (mc.level == null) {
            // Not in a world yet: once the game has settled on a menu, open the test world once.
            this.menuTicks++;
            if (this.menuTicks % 100 == 0) {
                log("waiting: screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName())
                        + " overlay=" + (mc.getOverlay() != null));
            }
            if (this.opening && mc.screen instanceof BackupConfirmScreen backup) {
                // A world made by the dedicated server asks whether to back up before opening: no.
                String skip = I18n.get("selectWorld.backupJoinSkipButton");
                for (var child : backup.children()) {
                    if (child instanceof Button button && button.getMessage().getString().equals(skip)) {
                        button.onPress();
                    }
                }
            }
            if (!this.opening && mc.getOverlay() == null && mc.screen != null && this.menuTicks > 100) {
                this.opening = true;
                log("opening world " + WORLD);
                mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
            }
            return;
        }

        if (mc.player == null || this.next >= this.steps.size()) {
            return;
        }

        if (this.wait > 0) {
            this.wait--;
            return;
        }

        Step step = this.steps.get(this.next++);
        step.action().run();
        this.wait = step.waitAfter();
    }
}
