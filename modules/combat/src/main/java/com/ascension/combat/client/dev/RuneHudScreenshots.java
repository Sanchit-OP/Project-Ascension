package com.ascension.combat.client.dev;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.mechanics.CombatStackState;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.Runes;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A development aid, dormant unless a file named {@code rune-hud-screenshots.txt} sits in the game
 * directory: opens the singleplayer world {@code runes-test}, saves a rune page whose runes have
 * running state (Conqueror's Resolve, Second Wind, Relentless Strikes, Plating, Riposte), forces
 * each state on the server, and photographs the HUD's rune chips at GUI scales 3, 4 and 2 under
 * {@code <game directory>/screenshots/}, then quits. It exists so the chips can be seen without
 * playing (ADR-0008: nothing is done until it is observed in a running client).
 *
 * <p>Conqueror's Resolve clears its stack every tick the player is not in combat, so the stack is
 * re-applied after it each tick for the length of the run ({@link #holdState}).
 */
public final class RuneHudScreenshots {

    private static final DevScript SCRIPT = new DevScript("rune-hud-screenshots");
    private static String pending;
    private static int guiScaleBefore;
    private static volatile boolean holding;

    private RuneHudScreenshots() {
    }

    public static void register(IEventBus gameBus) {
        if (!DevScript.armed("rune-hud-screenshots.txt")) {
            return;
        }
        plan();
        gameBus.addListener(RuneHudScreenshots::onTick);
        gameBus.addListener(RuneHudScreenshots::onGuiRendered);
        gameBus.addListener(EventPriority.LOW, RuneHudScreenshots::holdState);
    }

    private static void plan() {
        add(160, () -> {
            Minecraft mc = Minecraft.getInstance();
            guiScaleBefore = mc.options.guiScale().get();
            mc.options.pauseOnLostFocus = false;
        });
        add(40, () -> resizeFor(3));
        add(30, RuneHudScreenshots::savePage);
        add(40, () -> holding = true);
        add(2, RuneHudScreenshots::openWindows);
        add(6, RuneHudScreenshots::logProbes);
        add(6, () -> shoot("runes-3"));
        add(50, () -> shoot("runes-3-later"));
        add(30, () -> resizeFor(4));
        add(14, () -> shoot("runes-4"));
        add(30, () -> resizeFor(2));
        add(14, () -> shoot("runes-2"));
        // The whole HUD together at scale 3: oxygen running low and nearly gone (the atmosphere
        // module's own debug commands), then the config: everything bigger, then one element hidden.
        add(30, () -> resizeFor(3));
        add(5, () -> command("ascension atmosphere debug vacuum"));
        add(5, () -> command("ascension atmosphere debug oxygen 70"));
        add(14, () -> shoot("all-warning-3"));
        add(8, () -> command("ascension atmosphere debug oxygen 24"));
        add(14, () -> shoot("all-critical-3"));
        add(4, () -> command("ascension atmosphere debug clear"));
        add(30, () -> hudConfig("scale", 1.35));
        add(14, () -> shoot("all-scaled-3"));
        add(2, () -> hudConfig("scale", 1.0));
        add(2, () -> hudHide("ascension_combat:rune_chips"));
        add(14, () -> shoot("all-hidden-3"));
        add(2, () -> hudHide(null));
        add(10, () -> {
            holding = false;
            Minecraft mc = Minecraft.getInstance();
            mc.options.guiScale().set(guiScaleBefore);
            mc.options.save();
            mc.stop();
        });
    }

    private static void add(int waitAfter, Runnable action) {
        SCRIPT.add(waitAfter, action);
    }

    private static void resizeFor(int guiScale) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.guiScale().set(guiScale);
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
        mc.resizeDisplay();
    }

    /** Runs a command as the player, with full permission, on the singleplayer server. */
    private static void command(String command) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
        });
    }

    /**
     * Sets a number in the HUD module's client config. Combat does not depend on the HUD, so this
     * reaches it by name, which is fine for a dev script and keeps the dependency out of the build.
     */
    private static void hudConfig(String field, double value) {
        try {
            Class<?> config = Class.forName("com.ascension.hud.internal.HudConfig");
            Object instance = config.getField("INSTANCE").get(null);
            Object entry = config.getField(field).get(instance);
            entry.getClass().getMethod("set", Object.class).invoke(entry, value);
        } catch (ReflectiveOperationException e) {
            System.out.println("[rune-hud-screenshots] could not set HUD config " + field + ": " + e);
        }
    }

    /** Hides one HUD element by id in the HUD module's client config, or none when {@code id} is null. */
    private static void hudHide(String id) {
        try {
            Class<?> config = Class.forName("com.ascension.hud.internal.HudConfig");
            Object instance = config.getField("INSTANCE").get(null);
            Object entry = config.getField("hiddenElements").get(instance);
            entry.getClass().getMethod("set", Object.class).invoke(entry, id == null ? List.of() : List.of(id));
        } catch (ReflectiveOperationException e) {
            System.out.println("[rune-hud-screenshots] could not set hidden elements: " + e);
        }
    }

    private static void shoot(String name) {
        pending = name;
    }

    private static ServerPlayer player(MinecraftServer server) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    /** A Vanguard page with Conqueror's Resolve, Second Wind and Relentless Strikes, and Bulwark's Plating and Riposte. */
    private static void savePage() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player == null) {
                return;
            }
            player.setGameMode(GameType.SURVIVAL);
            // Every slot open, so the keystone and second row count: a new player has them locked.
            com.ascension.combat.internal.rune.RuneConfig.INSTANCE.unlocks.set(
                    com.ascension.combat.internal.rune.RuneConfig.Unlocks.ALL);
            RunePage page = new RunePage(Optional.of(RunePath.VANGUARD), Optional.of(Runes.id("conquerors_resolve")),
                    List.of(Runes.id("second_wind"), Runes.id("relentless_strikes")),
                    Optional.of(RunePath.BULWARK), List.of(Runes.id("plating"), Runes.id("riposte")),
                    List.of(Runes.id("shard_offence_attack_damage"), Runes.id("shard_flex_armor"),
                            Runes.id("shard_defence_health")));
            // Replacing runes costs Rune Chalk and levels, so a save without them is refused and the
            // old page stays: pay first, then save.
            player.getInventory().add(new net.minecraft.world.item.ItemStack(
                    com.ascension.combat.internal.rune.RuneContent.RUNE_CHALK.get(), 4));
            player.giveExperienceLevels(20);
            Runes.save(player, page);
            Runes.sync(player);
            System.out.println("[rune-hud-screenshots] page saved: " + player.getData(
                    com.ascension.combat.internal.CombatAttachments.RUNE_PAGE).page());
        });
    }

    /** Starts the cooldown and the two windows, long enough to outlast the shots. */
    private static void openWindows() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player == null) {
                return;
            }
            long now = player.level().getGameTime();
            player.getData(RuneAttachments.SECOND_WIND_COOLDOWN).activateUntil(now + 400);
            player.getData(RuneAttachments.PLATING).start(now, 3, 300, 900);
            player.getData(RuneAttachments.RIPOSTE).activateUntil(now + 120);
        });
    }

    /** Prints what the server reads for the player's runes, and what this client holds, so a missing chip can be explained. */
    private static void logProbes() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player == null) {
                return;
            }
            var page = player.getData(com.ascension.combat.internal.CombatAttachments.RUNE_PAGE);
            System.out.println("[rune-hud-screenshots] page=" + page.page());
            for (var e : com.ascension.combat.internal.rune.hud.RuneHudProbes.read(player, player.level().getGameTime())) {
                System.out.println("[rune-hud-screenshots] server entry " + e);
            }
            System.out.println("[rune-hud-screenshots] client holds "
                    + com.ascension.combat.client.hud.ClientRuneHud.size() + " entries");
        });
    }

    /** After the rune's own tick, and before the HUD sync reads it: put the stacks back, so a player who is not in combat still shows them. */
    private static void holdState(PlayerTickEvent.Post event) {
        if (!holding || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        CombatStackState conquerors = player.getData(RuneAttachments.CONQUERORS_RESOLVE);
        conquerors.clear();
        for (int i = 0; i < 9; i++) {
            conquerors.tryGainStack(now, 10, 0L);
        }
        StackingChargeState relentless = player.getData(RuneAttachments.RELENTLESS_STRIKES);
        relentless.clear();
        for (int i = 0; i < 3; i++) {
            relentless.refresh(4, now + 100);
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        SCRIPT.tick(Minecraft.getInstance());
    }

    private static void onGuiRendered(RenderGuiEvent.Post event) {
        if (pending == null) {
            return;
        }
        String name = pending;
        pending = null;
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "hud-" + name + ".png", mc.getMainRenderTarget(),
                message -> System.out.println("[rune-hud-screenshots] " + name + ": " + message.getString()));
    }
}
