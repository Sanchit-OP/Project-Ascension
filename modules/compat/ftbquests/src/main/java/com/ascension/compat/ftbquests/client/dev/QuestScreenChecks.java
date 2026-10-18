package com.ascension.compat.ftbquests.client.dev;

import com.ascension.compat.ftbquests.client.ChartLayout;
import com.ascension.compat.ftbquests.client.ChartMath;
import com.ascension.compat.ftbquests.client.ChartOverlay;
import com.ascension.compat.ftbquests.internal.dev.CheckReport;
import com.mojang.blaze3d.platform.NativeImage;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A development aid, dormant unless a file named {@code quest-screen-checks.txt} sits in the game
 * directory: opens the singleplayer world {@code runes-test}, then opens the real quest book and
 * photographs it, and checks the "you are here" marker where it should be, under
 * {@code <game directory>/screenshots/quest-book/}, printing {@code PASS} or {@code FAIL} lines tagged
 * {@code [quest-screen]}, and quits. It exists so the book can be seen, and the marker proved, without
 * playing (ADR-0008: nothing is done until it is observed in a running client).
 *
 * <p>The run: on Earth with nothing done (the pages as a new player meets them, a faint ring where a
 * world is still a rumour, the marker around Earth); with the oxygen-tank quest done (the Moon is named);
 * flying in space at the Moon's position (the marker is a point with an arrow, over the Moon); standing on
 * the Moon (it is visited, a halo, the marker around it, and the next world is named). Each stop is
 * photographed and the overlay's pixels are read back.
 *
 * <p>Quests is driven through {@link QuestBookDriver}. Needs the save {@code runes-test} in
 * {@code run/client/saves} (docs/technical/dev-environment.md).
 */
public final class QuestScreenChecks {

    private static final String TRIGGER = "quest-screen-checks.txt";
    private static final String WORLD = "runes-test";
    private static final CheckReport REPORT = new CheckReport("quest-screen");

    private static final ResourceLocation SPACE = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "space");

    private record Step(int waitAfter, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static int wait;
    private static int menuTicks;
    private static boolean opening;

    private QuestScreenChecks() {
    }

    public static void register(IEventBus gameBus) {
        if (!CheckReport.consumeTrigger(TRIGGER)) {
            return;
        }
        plan();
        gameBus.addListener(QuestScreenChecks::onTick);
    }

    // ------------------------------------------------------------------------------------------
    // The plan
    // ------------------------------------------------------------------------------------------

    private static void plan() {
        add(160, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
            mc.resizeDisplay();
        });
        add(10, () -> command("gamemode creative"));
        add(10, () -> {
            REPORT.expect("the chart layout was read", ChartOverlay.layout() != null);
            REPORT.expect("our texture is the one registered for the quest book's overlay image",
                    ChartOverlay.isRegistered());
        });

        // ---- Earth, nothing done.
        add(30, () -> checkMarkerOnWorld("Earth", "minecraft:overworld"));
        add(10, () -> openAt("First Days", "Open the Ledger"));
        add(40, () -> shoot("first-days-fresh"));
        add(10, () -> openAt("Planets", "Earth"));
        add(40, () -> shoot("planets-fresh"));

        // ---- The oxygen tank done: the Moon is named.
        add(10, QuestScreenChecks::closeScreen);
        add(10, () -> complete("First Days", "Open the Ledger"));
        add(4, () -> complete("First Days", "Know your numbers"));
        add(4, () -> complete("First Days", "First iron"));
        add(4, () -> complete("First Days", "Titanium"));
        add(4, () -> complete("First Days", "A breath of your own"));
        add(40, () -> expectDone("First Days", "A breath of your own"));
        add(4, () -> openAt("First Days", "Open the Ledger"));
        add(40, () -> shoot("first-days-progress"));
        add(10, () -> openAt("Planets", "Earth"));
        add(40, () -> shoot("planets-moon-known"));
        add(10, QuestScreenChecks::closeScreen);

        // ---- Flying in space at the Moon's position.
        add(10, () -> command("execute in ascension_worlds:space run tp @s 4000 64 0 0 0"));
        add(80, () -> checkMarkerInSpace(4000, 0));
        add(10, () -> openAt("Planets", "Earth"));
        add(40, () -> shoot("planets-space-at-moon"));
        add(10, QuestScreenChecks::closeScreen);
        add(10, () -> command("execute in ascension_worlds:space run tp @s 1500 64 -2200 135 0"));
        add(60, () -> checkMarkerInSpace(1500, -2200));
        add(10, () -> openAt("Planets", "Earth"));
        add(40, () -> shoot("planets-space-between"));
        add(10, QuestScreenChecks::closeScreen);

        // ---- On the Moon: visited, and the next world is named.
        add(10, () -> command("execute in ascension_worlds:moon run tp @s 0 200 0"));
        add(240, () -> checkMarkerOnWorld("the Moon", "ascension_worlds:moon"));
        add(10, () -> expectDone("Planets", "The Moon"));
        add(10, () -> openAt("Planets", "Earth"));
        add(40, () -> shoot("planets-moon-visited"));
        add(10, QuestScreenChecks::closeScreen);

        add(10, () -> {
            REPORT.finish();
            Minecraft.getInstance().stop();
        });
    }

    private static void add(int waitAfter, Runnable action) {
        STEPS.add(new Step(waitAfter, action));
    }

    // ------------------------------------------------------------------------------------------
    // The spine: open the world, then run the steps
    // ------------------------------------------------------------------------------------------

    private static void onTick(ClientTickEvent.Post event) {
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
                REPORT.print("opening world " + WORLD);
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
        try {
            step.action().run();
        } catch (Throwable t) {
            REPORT.fail("a step threw " + t);
        }
        wait = step.waitAfter();
    }

    // ------------------------------------------------------------------------------------------
    // Things the plan does
    // ------------------------------------------------------------------------------------------

    private static void command(String command) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) {
                return;
            }
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
        });
    }

    /** The marker is the ring around a world's sphere: nothing in the middle, a firm ring seventy-odd pixels out. */
    private static void checkMarkerOnWorld(String name, String dimension) {
        ChartLayout layout = ChartOverlay.layout();
        if (layout == null) {
            REPORT.fail("no layout, so no marker on " + name);
            return;
        }
        var planet = layout.planets().stream()
                .filter(p -> p.surface().toString().equals(dimension)).findFirst();
        if (planet.isEmpty()) {
            REPORT.fail(dimension + " is not on the chart");
            return;
        }
        int x = (int) Math.round(planet.get().pixelX());
        int y = (int) Math.round(planet.get().pixelY());
        REPORT.expect("on " + name + " the marker is a ring around it (" + ChartOverlay.alphaAt(x + 76, y) + ", "
                        + ChartOverlay.alphaAt(x, y - 76) + ") and nothing in the middle (" + ChartOverlay.alphaAt(x, y) + ")",
                ChartOverlay.alphaAt(x + 76, y) > 200 && ChartOverlay.alphaAt(x, y - 76) > 200
                        && ChartOverlay.alphaAt(x, y) == 0);
    }

    /** In space the marker is a point at the player's real place with an arrow ahead of it. */
    private static void checkMarkerInSpace(double blocksX, double blocksZ) {
        ChartLayout layout = ChartOverlay.layout();
        Minecraft mc = Minecraft.getInstance();
        if (layout == null || mc.level == null || mc.player == null) {
            REPORT.fail("cannot check the marker in space");
            return;
        }
        boolean inSpace = mc.level.dimension().location().equals(SPACE);
        REPORT.expect("the player is in space (" + mc.level.dimension().location() + ")", inSpace);
        double[] at = ChartMath.project(layout, mc.player.getX(), mc.player.getZ());
        ChartMath.Marker marker = ChartMath.locate(layout, mc.level.dimension().location(), mc.player.getX(),
                mc.player.getZ(), mc.player.getYRot()).orElse(null);
        REPORT.expect("the player stands about where the check sent them (" + Math.round(mc.player.getX()) + ", "
                + Math.round(mc.player.getZ()) + ")",
                Math.abs(mc.player.getX() - blocksX) < 40 && Math.abs(mc.player.getZ() - blocksZ) < 40);
        if (marker == null) {
            REPORT.fail("no marker in space");
            return;
        }
        int x = (int) Math.round(marker.x());
        int y = (int) Math.round(marker.y());
        int ahead = 25;
        int ax = (int) Math.round(x + ahead * Math.cos(marker.heading()));
        int ay = (int) Math.round(y + ahead * Math.sin(marker.heading()));
        REPORT.expect("in space the marker is a bright point at " + x + "," + y + " (" + ChartOverlay.alphaAt(x, y)
                        + ") with its arrow ahead at " + ax + "," + ay + " (" + ChartOverlay.alphaAt(ax, ay) + ")",
                ChartOverlay.alphaAt(x, y) > 200 && ChartOverlay.alphaAt(ax, ay) > 150);
        REPORT.expect("the projection put the player where the layout says (" + Math.round(at[0]) + ","
                + Math.round(at[1]) + ")", Math.abs(at[0] - x) < 1.0 && Math.abs(at[1] - y) < 1.0);
    }

    // ------------------------------------------------------------------------------------------
    // The book
    // ------------------------------------------------------------------------------------------

    private static void openAt(String chapterTitle, String title) {
        try {
            Object quest = QuestBookDriver.quest(chapterTitle, title);
            if (quest == null) {
                REPORT.fail("the book has no quest '" + title + "' on '" + chapterTitle + "'");
                return;
            }
            QuestBookDriver.open(quest);
        } catch (ReflectiveOperationException e) {
            REPORT.fail("could not open the book at '" + title + "': " + e);
        }
    }

    private static void closeScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    private static void complete(String chapterTitle, String title) {
        try {
            Object quest = QuestBookDriver.quest(chapterTitle, title);
            if (quest == null) {
                REPORT.fail("the book has no quest '" + title + "' to complete");
                return;
            }
            command("ftbquests change_progress @s complete " + QuestBookDriver.code(quest));
        } catch (ReflectiveOperationException e) {
            REPORT.fail("could not complete '" + title + "': " + e);
        }
    }

    private static void expectDone(String chapterTitle, String title) {
        try {
            REPORT.expect("'" + title + "' is done", QuestBookDriver.isDone(QuestBookDriver.quest(chapterTitle, title)));
        } catch (ReflectiveOperationException e) {
            REPORT.fail("could not read whether '" + title + "' is done: " + e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Photographs
    // ------------------------------------------------------------------------------------------

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Path folder = FMLPaths.GAMEDIR.get().resolve("screenshots").resolve("quest-book");
        try {
            Files.createDirectories(folder);
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(folder.resolve(name + ".png"));
            }
            REPORT.print("photographed " + name + " (screen: "
                    + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()) + ")");
        } catch (IOException e) {
            REPORT.fail("could not write " + name + ".png: " + e);
        }
    }
}
