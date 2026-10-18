package com.ascension.hud.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A development aid, dormant unless a file named {@code hud-screenshots.txt} sits in the game
 * directory: when it does, this opens the singleplayer world {@code runes-test}, puts the player in
 * a few states (full health; a hit with a shield, regeneration and a hungry stomach) and saves a
 * screenshot of the HUD at GUI scales 2, 3 and 4 under {@code <game directory>/screenshots/}, then
 * quits. It exists so the HUD can be seen without a person playing (ADR-0008: nothing is done
 * until it is observed in a running client).
 *
 * <p>It changes the player on the server side of the singleplayer world, the same way the game
 * would; it never touches the mouse.
 */
public final class HudScreenshots {

    private static final String WORLD = "runes-test";

    private record Step(int waitAfter, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static int wait;
    private static int menuTicks;
    private static boolean opening;
    private static String pending;
    private static int guiScaleBefore;

    private HudScreenshots() {
    }

    public static void register(IEventBus gameBus) {
        if (!armed("hud-screenshots.txt")) {
            return;
        }
        plan();
        gameBus.addListener(HudScreenshots::onTick);
        gameBus.addListener(HudScreenshots::onGuiRendered);
    }

    private static boolean armed(String name) {
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

    private static void plan() {
        add(160, () -> {
            Minecraft mc = Minecraft.getInstance();
            guiScaleBefore = mc.options.guiScale().get();
            mc.options.pauseOnLostFocus = false;
        });
        for (int scale : new int[] {3, 2, 4}) {
            add(40, () -> resizeFor(scale));
            // Full health, a clean slate.
            add(40, () -> state(1.0f, 0.0f, 20, 27, false));
            add(4, () -> shoot("full-" + scale));
            // A hit: health drops from 70% to 30%, so the red part is showing; a shield; regeneration;
            // a nearly empty stomach.
            add(25, () -> state(0.7f, 0.0f, 5, 27, false));
            add(8, () -> state(0.3f, 4.0f, 5, 27, true));
            add(4, () -> shoot("hit-" + scale));
        }
        // Oxygen, by the atmosphere module's own debug commands: running down, nearly gone, and
        // climbing back in good air.
        add(40, () -> resizeFor(3));
        add(20, () -> state(1.0f, 0.0f, 20, 27, false));
        add(5, () -> command("ascension atmosphere debug vacuum"));
        add(5, () -> command("ascension atmosphere debug oxygen 70"));
        add(14, () -> shoot("o2-warning-3"));
        add(12, () -> command("ascension atmosphere debug oxygen 24"));
        add(14, () -> shoot("o2-critical-3"));
        add(4, () -> command("ascension atmosphere debug clear"));
        add(3, () -> command("ascension atmosphere debug oxygen 30"));
        add(14, () -> shoot("o2-quiet-3"));
        add(30, () -> resizeFor(2));
        add(5, () -> command("ascension atmosphere debug vacuum"));
        add(3, () -> command("ascension atmosphere debug oxygen 24"));
        add(14, () -> shoot("o2-critical-2"));
        add(4, () -> command("ascension atmosphere debug clear"));
        add(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.guiScale().set(guiScaleBefore);
            mc.options.save();
            mc.stop();
        });
    }

    private static void add(int waitAfter, Runnable action) {
        STEPS.add(new Step(waitAfter, action));
    }

    private static void log(String message) {
        System.out.println("[hud-screenshots] " + message);
    }

    /** A window the HUD fits at this GUI scale; the scale option does the rest. */
    private static void resizeFor(int guiScale) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.guiScale().set(guiScale);
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
        mc.resizeDisplay();
    }

    /**
     * Puts the player in a state, on the server: survival, a share of maximum health, absorption,
     * food, an experience level, and optionally Regeneration II.
     */
    private static void state(float healthShare, float absorption, int food, int level, boolean regeneration) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player == null) {
                return;
            }
            player.setGameMode(GameType.SURVIVAL);
            player.removeAllEffects();
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * healthShare));
            player.setAbsorptionAmount(absorption);
            player.getFoodData().setFoodLevel(food);
            player.giveExperienceLevels(level - player.experienceLevel);
            if (regeneration) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            }
        });
    }

    /** Runs a command as the player, with full permission, on the singleplayer server. */
    private static void command(String command) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
        });
    }

    private static void shoot(String name) {
        pending = name;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            menuTicks++;
            if (menuTicks % 100 == 0) {
                log("waiting: screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName()));
            }
            if (opening && mc.screen instanceof BackupConfirmScreen backup) {
                String skip = I18n.get("selectWorld.backupJoinSkipButton");
                for (var child : backup.children()) {
                    if (child instanceof Button button && button.getMessage().getString().equals(skip)) {
                        button.onPress();
                    }
                }
            }
            if (!opening && mc.getOverlay() == null && mc.screen != null && menuTicks > 100) {
                opening = true;
                log("opening world " + WORLD);
                mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
            }
            return;
        }
        if (mc.player == null || next >= STEPS.size()) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.get(next++);
        step.action().run();
        wait = step.waitAfter();
    }

    /** Photographs once the HUD is fully drawn for the frame, so it is in the picture. */
    private static void onGuiRendered(RenderGuiEvent.Post event) {
        if (pending == null) {
            return;
        }
        String name = pending;
        pending = null;
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "hud-" + name + ".png", mc.getMainRenderTarget(),
                message -> log(name + ": " + message.getString()));
    }
}
