package com.ascension.combat.client.dev;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.client.RuneScreen;
import com.ascension.combat.client.rune.OrreryLayout;
import com.ascension.combat.client.rune.OrreryLayout.Point;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.rune.RuneContent;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageState;
import com.ascension.combat.internal.rune.Runes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A development aid, dormant unless a file named {@code rune-screenshots.txt} sits in the game
 * directory: when it does, this opens the singleplayer world {@code runes-test} from the title
 * screen, then opens the rune screen at GUI scales
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

    private static final DevScript SCRIPT = new DevScript("rune-screenshots");
    private static String pending;
    private static int guiScaleBefore;
    private static RuneScreen screen;

    private RuneScreenshots() {
    }

    public static void register(IEventBus gameBus) {
        if (!DevScript.armed("rune-screenshots.txt")) {
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
        add(30, () -> resizeFor(3));
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
        // unsaved choices: Vein Breaker in place of Cadence (a swap, so the cost shows), then the dial turning to Reaver
        add(2, () -> screen.devHover(-1000, -1000));
        add(3, () -> screen.devClick(OrreryLayout.primaryKeystone(2, true, 0).x(), OrreryLayout.primaryKeystone(2, true, 0).y()));
        add(10, () -> shoot("changed-3"));
        add(3, () -> screen.devClick(OrreryLayout.primaryTab(1).x(), OrreryLayout.primaryTab(1).y()));
        add(4, () -> shoot("turning-3"));
        add(14, () -> shoot("reaver-3"));
        // back to the saved page, then take Blooded off: Save asks before throwing its growth away
        add(3, () -> screen.devClick(330, 11));
        add(3, () -> screen.devClick(OrreryLayout.primaryMinor(0, 2, 0).x(), OrreryLayout.primaryMinor(0, 2, 0).y()));
        add(3, () -> screen.devClick(372, 11));
        add(6, () -> shoot("confirm-3"));
        add(3, () -> screen.devClick(270, 118));
        add(14, () -> shoot("refused-3"));
        add(3, RuneScreenshots::logServerPage);
        add(3, RuneScreenshots::grantCost);
        add(8, () -> screen.devClick(372, 11));
        add(3, () -> screen.devClick(270, 118));
        add(14, () -> shoot("saved-3"));
        add(3, RuneScreenshots::logServerPage);
        // the three other paths, each as a primary with a secondary: the page, then every rune's card
        for (PathPage p : PATH_PAGES) {
            add(2, () -> mc().setScreen(null));
            add(3, RuneScreenshots::grantCost);
            add(8, () -> savePathPage(p));
            add(3, RuneScreenshots::logServerPage);
            add(20, () -> open(3));
            add(6, () -> shoot(p.name() + "-page"));
            int n = 0;
            for (Point at : cardPoints()) {
                add(2, () -> screen.devHover(at.x(), at.y()));
                add(6, shootNumbered(p.name() + "-card", n++));
            }
            add(2, () -> screen.devHover(-1000, -1000));
        }
        add(2, () -> mc().setScreen(null));
        add(30, () -> resizeFor(2));
        add(15, () -> open(2));
        add(6, () -> shoot("page-2"));
        add(2, () -> mc().setScreen(null));
        add(40, () -> resizeFor(4));
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
        SCRIPT.add(waitAfter, action);
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

    /** A window the panel fits at this GUI scale: 400 x 224 GUI pixels is 1200 x 672 at scale 3, 1600 x 896 at 4. */
    private static void resizeFor(int guiScale) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.guiScale().set(guiScale);
        int width = guiScale >= 4 ? 1640 : 1280;
        int height = guiScale >= 4 ? 920 : 720;
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), width, height);
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

    /** A page to save for the photographs: a primary with its keystone and two minors, a secondary with two, three shards. */
    private record PathPage(String name, RunePath primary, String keystone, List<String> minors, RunePath secondary,
            List<String> secondaryMinors) {
    }

    private static final List<PathPage> PATH_PAGES = List.of(
            new PathPage("reaver", RunePath.REAVER, "deadeye", List.of("pounce", "trophy_hunter"), RunePath.AETHERWRIGHT,
                    List.of("clarity", "quickening")),
            new PathPage("aetherwright", RunePath.AETHERWRIGHT, "starfall", List.of("attunement", "gathering_charge"), RunePath.BULWARK,
                    List.of("plating", "riposte")),
            new PathPage("bulwark", RunePath.BULWARK, "brace", List.of("deep_roots", "revitalize"), RunePath.REAVER,
                    List.of("vanish", "overkill")));

    /** Where every card of a path's dial can be summoned: three keystones, then six minors. */
    private static List<Point> cardPoints() {
        List<Point> points = new ArrayList<>();
        points.add(OrreryLayout.primaryKeystone(0, false, 0));
        points.add(OrreryLayout.primaryKeystone(1, false, 0));
        points.add(OrreryLayout.primaryKeystone(0, true, 0));
        for (int row = 0; row < 2; row++) {
            for (int place = 0; place < 3; place++) {
                points.add(OrreryLayout.primaryMinor(row, place, 0));
            }
        }
        return points;
    }

    private static Runnable shootNumbered(String name, int n) {
        return () -> shoot(name + "-" + n);
    }

    private static void savePathPage(PathPage p) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> {
            RunePage page = new RunePage(Optional.of(p.primary()), Optional.of(Runes.id(p.keystone())),
                    p.minors().stream().map(Runes::id).toList(), Optional.of(p.secondary()),
                    p.secondaryMinors().stream().map(Runes::id).toList(),
                    List.of(Runes.id("shard_offence_attack_damage"), Runes.id("shard_flex_magic_resist"), Runes.id("shard_defence_health")));
            Runes.save(player, page);
            Runes.sync(player);
        });
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
                    List.of(blooded, Runes.id("giant_slayer")), Optional.empty(), List.of(),
                    List.of(Runes.id("shard_offence_attack_speed"), Runes.id("shard_flex_armor")));
            Runes.save(player, page);
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            for (String kind : List.of("zombie", "skeleton", "spider", "creeper", "witch", "enderman", "slime")) {
                state.grow(blooded, ResourceLocation.withDefaultNamespace(kind), 12);
            }
            RuneModifiers.refresh(player);
            Runes.sync(player);
        });
    }

    /** Gives the singleplayer player one Rune Chalk and five levels, through the server, so a swap can be paid for. */
    private static void grantCost() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> {
            player.getInventory().add(new ItemStack(RuneContent.RUNE_CHALK.get(), 1));
            player.giveExperienceLevels(5);
        });
    }

    /** Prints what the server holds for the player: the page, the growth, the chalk, the levels, Blooded's omnivamp. */
    private static void logServerPage() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> {
            RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
            System.out.println("[rune-screenshots] SERVER page: keystone=" + state.page().keystone()
                    + " primaryMinors=" + state.page().primaryMinors() + " shards=" + state.page().shards()
                    + " growth=" + state.growthCounts()
                    + " chalk=" + player.getInventory().countItem(RuneContent.RUNE_CHALK.get())
                    + " levels=" + player.experienceLevel
                    + " omnivamp=" + player.getAttributeValue(CombatAttributes.OMNIVAMP));
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        SCRIPT.tick(Minecraft.getInstance());
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
