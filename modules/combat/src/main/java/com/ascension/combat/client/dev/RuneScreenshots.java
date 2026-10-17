package com.ascension.combat.client.dev;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.client.RuneScreen;
import com.ascension.combat.client.rune.OrreryLayout;
import com.ascension.combat.client.rune.OrreryLayout.Point;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageState;
import com.ascension.combat.internal.rune.Runes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * A development aid, dormant unless a file named {@code rune-screenshots.txt} sits in the game
 * directory: when it does, and the client is in a world, this opens the rune screen at GUI scales
 * 2, 3 and 4 (a new player's empty page, then a real page, with hover cards) and saves a
 * screenshot of each under {@code <game directory>/screenshots/}, then quits the game. It exists so
 * the screen can be seen without a person clicking through it (ADR-0008: nothing is done until it
 * is observed in a running client), and it never touches the mouse: the screen is told where to
 * pretend the pointer is ({@link RuneScreen#devHover}).
 *
 * <p>It saves a page through the same {@link Runes#save} a player's Save does, in the singleplayer
 * world the client is in, so what it photographs is a page the server accepted.
 */
public final class RuneScreenshots {

    private record Step(int waitAfter, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int next;
    private static int wait;
    private static String pending;
    private static int guiScaleBefore;
    private static RuneScreen screen;

    private RuneScreenshots() {
    }

    public static void register(IEventBus gameBus) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve("rune-screenshots.txt");
        if (!Files.exists(trigger)) {
            return;
        }

        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return;
        }

        plan();
        gameBus.addListener(RuneScreenshots::onTick);
        gameBus.addListener(RuneScreenshots::onScreenRendered);
    }

    private static void plan() {
        add(160, () -> {
            Minecraft mc = Minecraft.getInstance();
            guiScaleBefore = mc.options.guiScale().get();
            mc.options.pauseOnLostFocus = false;
        });
        add(25, () -> open(3));
        add(6, () -> shoot("new-player-3"));
        add(2, () -> mc().setScreen(null));
        add(40, RuneScreenshots::savePage);
        add(25, () -> open(3));
        add(6, () -> shoot("page-3"));
        add(2, () -> screen.devHover(hoverKeystone().x(), hoverKeystone().y()));
        add(6, () -> shoot("card-keystone-3"));
        add(2, () -> screen.devHover(hoverMinor().x(), hoverMinor().y()));
        add(6, () -> shoot("card-minor-3"));
        add(2, () -> mc().setScreen(null));
        add(15, () -> open(2));
        add(6, () -> shoot("page-2"));
        add(2, () -> mc().setScreen(null));
        add(15, () -> open(4));
        add(6, () -> shoot("page-4"));
        add(2, () -> mc().setScreen(null));
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

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static Point hoverKeystone() {
        return OrreryLayout.primaryKeystone(1, false, 0);
    }

    private static Point hoverMinor() {
        return OrreryLayout.primaryMinor(0, 2, 0);
    }

    private static void open(int guiScale) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.guiScale().set(guiScale);
        mc.resizeDisplay();
        screen = new RuneScreen();
        mc.setScreen(screen);
    }

    private static void shoot(String name) {
        pending = name;
    }

    /** Saves a Vanguard page with Cadence, Blooded (seven kinds grown) and Giant Slayer, as a player's Save would. */
    private static void savePage() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> {
            ResourceLocation blooded = Runes.id("blooded");
            RunePage page = new RunePage(Optional.of(RunePath.VANGUARD), Optional.of(Runes.id("cadence")),
                    List.of(blooded, Runes.id("giant_slayer")), Optional.empty(), List.of(), List.of());
            Runes.save(player, page);
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            for (String kind : List.of("zombie", "skeleton", "spider", "creeper", "witch", "enderman", "slime")) {
                state.grow(blooded, ResourceLocation.withDefaultNamespace(kind), 12);
            }
            RuneModifiers.refresh(player);
            Runes.sync(player);
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || next >= STEPS.size()) {
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

    private static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (pending == null || !(event.getScreen() instanceof RuneScreen)) {
            return;
        }

        String name = pending;
        pending = null;
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "rune-" + name + ".png", mc.getMainRenderTarget(),
                message -> System.out.println("[rune-screenshots] " + name + ": " + message.getString()));
    }
}
