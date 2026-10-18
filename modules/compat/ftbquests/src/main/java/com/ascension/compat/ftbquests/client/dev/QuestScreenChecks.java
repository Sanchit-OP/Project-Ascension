package com.ascension.compat.ftbquests.client.dev;

import com.ascension.compat.ftbquests.client.ChartLayout;
import com.ascension.compat.ftbquests.client.ChartMath;
import com.ascension.compat.ftbquests.client.ChartOverlay;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
 * <p>Quests is driven by reflection, on purpose: this jar has no compile dependency on it.
 * Needs the save {@code runes-test} in {@code run/client/saves} (docs/technical/dev-environment.md).
 */
public final class QuestScreenChecks {

    private static final String TRIGGER = "quest-screen-checks.txt";
    private static final String WORLD = "runes-test";
    private static final String TAG = "[quest-screen] ";

    private static final ResourceLocation SPACE = ResourceLocation.fromNamespaceAndPath("ascension_worlds", "space");

    private record Step(int waitAfter, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static int wait;
    private static int menuTicks;
    private static boolean opening;
    private static int checks;
    private static int failures;

    private QuestScreenChecks() {
    }

    public static void register(IEventBus gameBus) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(TRIGGER);
        if (!Files.exists(trigger)) {
            return;
        }
        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
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
            expect("the chart layout was read", ChartOverlay.layout() != null);
            expect("our texture is the one registered for the quest book's overlay image", ChartOverlay.isRegistered());
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
            print("RESULT: " + checks + " checks, " + failures + " failures");
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
                print("opening world " + WORLD);
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
            fail("a step threw " + t);
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
            fail("no layout, so no marker on " + name);
            return;
        }
        var planet = layout.planets().stream()
                .filter(p -> p.surface().toString().equals(dimension)).findFirst();
        if (planet.isEmpty()) {
            fail(dimension + " is not on the chart");
            return;
        }
        int x = (int) Math.round(planet.get().pixelX());
        int y = (int) Math.round(planet.get().pixelY());
        expect("on " + name + " the marker is a ring around it (" + ChartOverlay.alphaAt(x + 76, y) + ", "
                        + ChartOverlay.alphaAt(x, y - 76) + ") and nothing in the middle (" + ChartOverlay.alphaAt(x, y) + ")",
                ChartOverlay.alphaAt(x + 76, y) > 200 && ChartOverlay.alphaAt(x, y - 76) > 200
                        && ChartOverlay.alphaAt(x, y) == 0);
    }

    /** In space the marker is a point at the player's real place with an arrow ahead of it. */
    private static void checkMarkerInSpace(double blocksX, double blocksZ) {
        ChartLayout layout = ChartOverlay.layout();
        Minecraft mc = Minecraft.getInstance();
        if (layout == null || mc.level == null || mc.player == null) {
            fail("cannot check the marker in space");
            return;
        }
        boolean inSpace = mc.level.dimension().location().equals(SPACE);
        expect("the player is in space (" + mc.level.dimension().location() + ")", inSpace);
        double[] at = ChartMath.project(layout, mc.player.getX(), mc.player.getZ());
        ChartMath.Marker marker = ChartMath.locate(layout, mc.level.dimension().location(), mc.player.getX(),
                mc.player.getZ(), mc.player.getYRot()).orElse(null);
        expect("the player stands about where the check sent them (" + Math.round(mc.player.getX()) + ", "
                + Math.round(mc.player.getZ()) + ")",
                Math.abs(mc.player.getX() - blocksX) < 40 && Math.abs(mc.player.getZ() - blocksZ) < 40);
        if (marker == null) {
            fail("no marker in space");
            return;
        }
        int x = (int) Math.round(marker.x());
        int y = (int) Math.round(marker.y());
        int ahead = 25;
        int ax = (int) Math.round(x + ahead * Math.cos(marker.heading()));
        int ay = (int) Math.round(y + ahead * Math.sin(marker.heading()));
        expect("in space the marker is a bright point at " + x + "," + y + " (" + ChartOverlay.alphaAt(x, y)
                        + ") with its arrow ahead at " + ax + "," + ay + " (" + ChartOverlay.alphaAt(ax, ay) + ")",
                ChartOverlay.alphaAt(x, y) > 200 && ChartOverlay.alphaAt(ax, ay) > 150);
        expect("the projection put the player where the layout says (" + Math.round(at[0]) + "," + Math.round(at[1]) + ")",
                Math.abs(at[0] - x) < 1.0 && Math.abs(at[1] - y) < 1.0);
    }

    // ------------------------------------------------------------------------------------------
    // FTB Quests, by reflection
    // ------------------------------------------------------------------------------------------

    private static Object file() throws ReflectiveOperationException {
        Class<?> type = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
        return type.getField("INSTANCE").get(null);
    }

    /** The quest on {@code chapterFile}'s page whose title is {@code title}, or null. */
    private static Object quest(String chapterTitle, String title) throws ReflectiveOperationException {
        Object file = file();
        Collection<?> chapters = (Collection<?>) file.getClass().getMethod("getAllChapters").invoke(file);
        for (Object chapter : chapters) {
            String chapterName = (String) chapter.getClass().getMethod("getRawTitle").invoke(chapter);
            if (!matches(chapter, chapterTitle, chapterName)) {
                continue;
            }
            Collection<?> quests = (Collection<?>) chapter.getClass().getMethod("getQuests").invoke(chapter);
            for (Object quest : quests) {
                if (matches(quest, title, (String) quest.getClass().getMethod("getRawTitle").invoke(quest))) {
                    return quest;
                }
            }
        }
        return null;
    }

    /** A title in the lang file comes back from getTitle(); the raw title is empty for ours. */
    private static boolean matches(Object object, String wanted, String raw) throws ReflectiveOperationException {
        Object component = object.getClass().getMethod("getTitle").invoke(object);
        String shown = component.getClass().getMethod("getString").invoke(component).toString();
        return wanted.equals(shown) || wanted.equals(raw);
    }

    private static void openAt(String chapterTitle, String title) {
        try {
            Object quest = quest(chapterTitle, title);
            if (quest == null) {
                fail("the book has no quest '" + title + "' on '" + chapterTitle + "'");
                return;
            }
            Class<?> fileClass = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            Method open = fileClass.getMethod("openGui", Class.forName("dev.ftb.mods.ftbquests.quest.Quest"), boolean.class);
            open.invoke(null, quest, false);
        } catch (ReflectiveOperationException e) {
            fail("could not open the book at '" + title + "': " + e);
        }
    }

    private static void closeScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    private static void complete(String chapterTitle, String title) {
        try {
            Object quest = quest(chapterTitle, title);
            if (quest == null) {
                fail("the book has no quest '" + title + "' to complete");
                return;
            }
            String code = (String) quest.getClass().getMethod("getCodeString").invoke(quest);
            command("ftbquests change_progress @s complete " + code);
        } catch (ReflectiveOperationException e) {
            fail("could not complete '" + title + "': " + e);
        }
    }

    private static void expectDone(String chapterTitle, String title) {
        try {
            Object quest = quest(chapterTitle, title);
            Object file = file();
            Object team = file.getClass().getField("selfTeamData").get(file);
            Class<?> objectType = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
            boolean done = quest != null
                    && (boolean) team.getClass().getMethod("isCompleted", objectType).invoke(team, quest);
            expect("'" + title + "' is done", done);
        } catch (ReflectiveOperationException e) {
            fail("could not read whether '" + title + "' is done: " + e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Photographs and reporting
    // ------------------------------------------------------------------------------------------

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Path folder = FMLPaths.GAMEDIR.get().resolve("screenshots").resolve("quest-book");
        try {
            Files.createDirectories(folder);
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(folder.resolve(name + ".png"));
            }
            print("photographed " + name + " (screen: "
                    + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()) + ")");
        } catch (IOException e) {
            fail("could not write " + name + ".png: " + e);
        }
    }

    private static void expect(String what, boolean ok) {
        checks++;
        if (ok) {
            print("PASS " + what);
        } else {
            failures++;
            print("FAIL " + what);
        }
    }

    private static void fail(String what) {
        checks++;
        failures++;
        print("FAIL " + what);
    }

    private static void print(String line) {
        System.out.println(TAG + line);
    }
}
