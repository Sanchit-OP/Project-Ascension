package com.ascension.combat.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A development aid, dormant unless a file named {@code gallery-shots.txt} sits in the game directory: it opens the
 * singleplayer world {@code runes-test}, then plays the script in {@code gallery-plan.txt} and quits. The script is for the
 * pictures on the CurseForge page, so the camera is put where the pictures are and a screenshot is saved under
 * {@code <game directory>/screenshots/gallery-<name>.png}. It never touches the mouse or the keyboard.
 *
 * <p>One command a line, {@code #} starts a comment:
 * <ul>
 *   <li>{@code size W H}, {@code fov N}, {@code dist CHUNKS}, {@code hide}, {@code show}: the window and the options;</li>
 *   <li>{@code cmd TEXT}: a server command run as the player with full permission ({@code cmd time set 18000});</li>
 *   <li>{@code tp DIM X Y Z YAW PITCH}: put the camera there, in any dimension;</li>
 *   <li>{@code ground OFFSET YAW PITCH}: stand OFFSET blocks above the surface where the camera already is;</li>
 *   <li>{@code wait TICKS}, {@code shot NAME}, {@code locate DIM STRUCTURE}, {@code bbox DIM STRUCTURE X Z} (the boxes of a
 *       structure started in the chunk of X Z), {@code quit};</li>
 *   <li>{@code view NAME DIM X Y Z YAW PITCH [WAIT]}: tp, wait for the chunks (default 120 ticks), shoot;</li>
 *   <li>{@code viewg NAME DIM X Z OFFSET YAW PITCH [WAIT]}: the same, but over the surface at X Z.</li>
 * </ul>
 */
public final class GalleryShots {

    private static final DevScript SCRIPT = new DevScript("gallery", "gallery-world");

    private GalleryShots() {
    }

    public static void register(IEventBus gameBus) {
        if (!DevScript.armed("gallery-shots.txt")) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(FMLPaths.GAMEDIR.get().resolve("gallery-plan.txt"));
        } catch (IOException e) {
            System.out.println("[gallery] no gallery-plan.txt: " + e);
            return;
        }

        SCRIPT.add(160, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.bobView().set(false);
        });
        for (String raw : lines) {
            String line = raw.contains("#") ? raw.substring(0, raw.indexOf('#')).trim() : raw.trim();
            if (!line.isEmpty()) {
                try {
                    plan(line.split("\\s+"), line);
                } catch (RuntimeException e) {
                    System.out.println("[gallery] cannot read the line '" + line + "': " + e);
                }
            }
        }
        gameBus.addListener(GalleryShots::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        SCRIPT.tick(Minecraft.getInstance());
    }

    private static void plan(String[] t, String line) {
        switch (t[0]) {
            case "size" -> SCRIPT.add(30, () -> {
                Minecraft mc = Minecraft.getInstance();
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), Integer.parseInt(t[1]), Integer.parseInt(t[2]));
            });
            case "fov" -> SCRIPT.add(2, () -> Minecraft.getInstance().options.fov().set(Integer.parseInt(t[1])));
            case "dist" -> SCRIPT.add(2, () -> Minecraft.getInstance().options.renderDistance().set(Integer.parseInt(t[1])));
            case "hide" -> SCRIPT.add(2, () -> Minecraft.getInstance().options.hideGui = true);
            case "show" -> SCRIPT.add(2, () -> Minecraft.getInstance().options.hideGui = false);
            case "cmd" -> SCRIPT.add(8, () -> command(line.substring(3).trim()));
            case "tp" -> SCRIPT.add(8, () -> command(teleport(t[1], t[2], t[3], t[4], t[5], t[6])));
            case "ground" -> SCRIPT.add(8, () -> ground(Integer.parseInt(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])));
            case "wait" -> SCRIPT.add(Integer.parseInt(t[1]), () -> { });
            case "shot" -> {
                SCRIPT.add(4, () -> { });
                SCRIPT.add(4, () -> shoot(t[1]));
            }
            case "locate" -> SCRIPT.add(4, () -> locate(t[1], t[2]));
            case "bbox" -> SCRIPT.add(4, () -> bbox(t[1], t[2], Integer.parseInt(t[3]), Integer.parseInt(t[4])));
            case "quit" -> SCRIPT.add(2, () -> Minecraft.getInstance().stop());
            case "view" -> {
                SCRIPT.add(8, () -> command(teleport(t[2], t[3], t[4], t[5], t[6], t[7])));
                SCRIPT.add(t.length > 8 ? Integer.parseInt(t[8]) : 120, () -> { });
                SCRIPT.add(4, () -> { });
                SCRIPT.add(4, () -> shoot(t[1]));
            }
            case "viewg" -> {
                SCRIPT.add(8, () -> command(teleport(t[2], t[3], "250", t[4], "0", "0")));
                SCRIPT.add(t.length > 8 ? Integer.parseInt(t[8]) : 120, () -> { });
                SCRIPT.add(8, () -> ground(Integer.parseInt(t[5]), Float.parseFloat(t[6]), Float.parseFloat(t[7])));
                SCRIPT.add(50, () -> { });
                SCRIPT.add(4, () -> { });
                SCRIPT.add(4, () -> shoot(t[1]));
            }
            default -> System.out.println("[gallery] unknown command: " + line);
        }
    }

    private static String teleport(String dim, String x, String y, String z, String yaw, String pitch) {
        return "execute in " + dim + " run tp @s " + x + " " + y + " " + z + " " + yaw + " " + pitch;
    }

    private static ServerPlayer player(MinecraftServer server) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void command(String text) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), text);
            }
        });
    }

    private static void ground(int offset, float yaw, float pitch) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ServerPlayer player = player(server);
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            int x = player.blockPosition().getX();
            int z = player.blockPosition().getZ();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + offset;
            System.out.println("[gallery] ground at " + x + " " + z + " is " + (y - offset) + ", camera at " + y);
            player.teleportTo(level, x + 0.5, y, z + 0.5, yaw, pitch);
        });
    }

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "gallery-" + name + ".png", mc.getMainRenderTarget(),
                message -> System.out.println("[gallery] " + name + ": " + message.getString()));
    }

    /** Prints the bounding box of a structure that started in the chunk holding X Z, and of each of its pieces. */
    private static void bbox(String dimension, String structure, int x, int z) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
            var registry = level == null ? null : level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Structure wanted = registry == null ? null : registry.get(ResourceLocation.parse(structure));
            if (wanted == null) {
                System.out.println("[gallery] bbox: no " + structure + " in " + dimension);
                return;
            }

            var starts = level.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(new BlockPos(x, 0, z)), s -> s == wanted);
            System.out.println("[gallery] bbox: " + starts.size() + " start(s) of " + structure + " at chunk of " + x + " " + z);
            for (var start : starts) {
                System.out.println("[gallery] bbox: whole " + start.getBoundingBox());
                for (var piece : start.getPieces()) {
                    System.out.println("[gallery] bbox: piece " + piece.getClass().getSimpleName() + " " + piece.getBoundingBox());
                }
            }
        });
    }

    /** Prints where the nearest structure of this kind is; the finished pictures use the numbers. */
    private static void locate(String dimension, String structure) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
            if (level == null) {
                System.out.println("[gallery] no dimension " + dimension);
                return;
            }

            var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(structure)));
            if (holder.isEmpty()) {
                System.out.println("[gallery] no structure " + structure);
                return;
            }

            var found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.<Structure>direct(holder.get()), BlockPos.ZERO, 200, false);
            System.out.println("[gallery] located " + structure + " in " + dimension + ": "
                    + (found == null ? "nothing within 200 chunks" : found.getFirst().toShortString()));
        });
    }
}
