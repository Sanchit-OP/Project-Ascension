package com.ascension.compat.ftbquests.internal.dev;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A development aid, dormant unless a file named {@code quest-checks.txt} sits in the game directory: on a
 * server it asks FTB Quests what it loaded from {@code config/ftbquests/quests} and prints {@code PASS} or
 * {@code FAIL} lines tagged {@code [quest-checks]}, then stops the server.
 *
 * <p>The question it answers is the one the generator cannot: <i>did the real FTB Quests accept these
 * files?</i> Every {@code id} in the generated chapter files must be an object the loaded quest file knows
 * (a file Quests could not read, or an object it dropped, shows up as an id it does not recognise), and
 * every item a task or reward names must be a real item rather than Quests' "missing item" stand-in.
 *
 * <p>Quests is reached by reflection, on purpose: this jar has no compile dependency on it (so a change
 * inside Quests cannot break the build), and a dev check that names Quests' classes by string needs
 * nothing from it but what it already exposes.
 */
public final class QuestChecks {

    private static final String TRIGGER = "quest-checks.txt";
    private static final String TAG = "[quest-checks] ";
    private static final Pattern ID = Pattern.compile("\\bid:\\s*\"([0-9A-F]{16})\"");

    /** Ticks to wait after the server starts: Quests loads its file as the server comes up. */
    private static final int SETTLE_TICKS = 60;

    private static MinecraftServer server;
    private static int ticks = -1;
    private static int checks;
    private static int failures;

    private QuestChecks() {
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
        gameBus.addListener((ServerStartedEvent event) -> {
            server = event.getServer();
            ticks = 0;
        });
        gameBus.addListener(QuestChecks::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (ticks < 0 || ++ticks < SETTLE_TICKS) {
            return;
        }
        ticks = -1;
        try {
            run();
        } catch (Throwable t) {
            fail("the check itself threw " + t);
        }
        print("RESULT: " + checks + " checks, " + failures + " failures");
        server.halt(false);
    }

    private static void run() throws ReflectiveOperationException, IOException {
        Class<?> fileClass = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
        Optional<?> loaded = (Optional<?>) fileClass.getMethod("getInstance").invoke(null);
        expect("FTB Quests has a loaded quest file", loaded.isPresent());
        if (loaded.isEmpty()) {
            return;
        }
        Object file = loaded.get();

        Path quests = FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
        Path chapters = quests.resolve("chapters");
        Set<String> expected = new HashSet<>();
        int chapterFiles = 0;
        try (Stream<Path> list = Files.list(chapters)) {
            for (Path p : (Iterable<Path>) list.filter(f -> f.toString().endsWith(".snbt"))::iterator) {
                chapterFiles++;
                Matcher m = ID.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) {
                    expected.add(m.group(1));
                }
            }
        }
        expect("there are generated chapter files (" + chapterFiles + ") and ids in them (" + expected.size() + ")",
                chapterFiles > 0 && !expected.isEmpty());

        Method getBase = file.getClass().getMethod("getBase", long.class);
        List<String> unknown = new ArrayList<>();
        for (String id : expected) {
            if (getBase.invoke(file, Long.parseUnsignedLong(id, 16)) == null) {
                unknown.add(id);
            }
        }
        expect("every id in the chapter files is an object Quests loaded"
                + (unknown.isEmpty() ? "" : " (not loaded: " + unknown.subList(0, Math.min(8, unknown.size())) + ")"),
                unknown.isEmpty());

        Collection<?> chapterList = (Collection<?>) file.getClass().getMethod("getAllChapters").invoke(file);
        expect("Quests loaded the same number of chapters as there are files (" + chapterList.size() + ")",
                chapterList.size() == chapterFiles);

        List<String> missing = new ArrayList<>();
        int stacks = 0;
        Collection<?> all = (Collection<?>) file.getClass().getMethod("getAllObjects").invoke(file);
        for (Object object : all) {
            ItemStack stack = stackOf(object);
            if (stack == null) {
                continue;
            }
            stacks++;
            String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (stack.isEmpty() || key.equals("ftbquests:missing_item") || key.equals("minecraft:air")) {
                missing.add(String.valueOf(object));
            }
        }
        expect("every item a task or reward names is a real item (" + stacks + " checked)"
                + (missing.isEmpty() ? "" : " (missing: " + missing.subList(0, Math.min(5, missing.size())) + ")"),
                missing.isEmpty());
    }

    /** The item a task or reward is about, or null if the object has none. */
    private static ItemStack stackOf(Object object) throws IllegalAccessException {
        String name = object.getClass().getSimpleName();
        String method = switch (name) {
            case "ItemTask" -> "getItemStack";
            case "ItemReward" -> "getItem";
            default -> null;
        };
        if (method == null) {
            return null;
        }
        try {
            return (ItemStack) object.getClass().getMethod(method).invoke(object);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            return ItemStack.EMPTY;
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
