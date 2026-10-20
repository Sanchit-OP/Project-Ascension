package com.ascension.worlds.client.dev;

import com.ascension.worlds.AscensionWorlds;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * A development aid, dormant unless a file named {@code belt-screenshots.txt} sits in the game directory: opens the
 * singleplayer world {@code belt-test}, flies the player through space to look at the End's asteroid belt from several
 * distances and heights, photographs each view under {@code <game directory>/screenshots/}, then quits. It exists so the
 * belt can be seen without playing (ADR-0008). Pure dev tooling: it touches no game state beyond teleporting the player.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
public final class BeltScreenshots {

    private static final String WORLD = "belt-test";
    /** The End's place in space (data/ascension_worlds/ascension_worlds/planet/end.json). */
    private static final double X = -10945;
    private static final double Z = 30070;

    private record Step(int waitAfter, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static boolean checked;
    private static boolean armed;
    private static boolean opening;
    private static int menuTicks;
    private static int next;
    private static int wait;
    private static String pending;

    private BeltScreenshots() {
    }

    private static void plan() {
        add(60, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false;
            command("gamemode spectator @s");
        });
        // name, distance south of the belt, height, pitch (positive looks down)
        view("far", 3000, 260, 4);
        view("far-oblique", 2400, 1100, 28);
        view("mid", 1200, 180, 8);
        view("mid-oblique", 1000, 600, 30);
        view("near", 520, 90, 7);
        view("edge-on", 1500, 2, 0);
        view("above", 100, 900, 80);
        add(20, () -> Minecraft.getInstance().stop());
    }

    private static void view(String name, double distance, double height, double pitch) {
        add(2, () -> command(String.format("execute in ascension_worlds:space run tp @s %.1f %.1f %.1f 180 %.1f",
                X, height, Z + distance, pitch)));
        add(80, () -> { });
        add(10, () -> pending = name);
    }

    private static void add(int waitAfter, Runnable action) {
        STEPS.add(new Step(waitAfter, action));
    }

    private static void command(String command) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            ServerPlayer player = mc.player == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
        });
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!checked) {
            checked = true;
            Path trigger = FMLPaths.GAMEDIR.get().resolve("belt-screenshots.txt");
            if (Files.exists(trigger)) {
                try {
                    Files.deleteIfExists(trigger);
                    armed = true;
                    plan();
                } catch (IOException e) {
                    armed = false;
                }
            }
        }
        if (!armed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            menuTicks++;
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
                System.out.println("[belt-screenshots] opening world " + WORLD);
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

    @SubscribeEvent
    public static void onGuiRendered(RenderGuiEvent.Post event) {
        if (pending == null) {
            return;
        }
        String name = pending;
        pending = null;
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "belt-" + name + ".png", mc.getMainRenderTarget(),
                message -> System.out.println("[belt-screenshots] " + name + ": " + message.getString()));
    }
}
