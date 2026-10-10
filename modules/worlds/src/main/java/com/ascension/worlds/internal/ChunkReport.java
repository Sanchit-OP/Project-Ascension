package com.ascension.worlds.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Vanilla's own debug report, per dimension, on a dedicated server. A client gets it from F3 + L
 * on its integrated server; a dedicated server has no command for it, yet it is the one place the
 * game writes down every loaded chunk with the ticket holding it ({@code chunks.csv}), plus its
 * entities and block entities. Added 2026-10-10 to find out what keeps pre-generated chunks
 * loaded (the Chunky crash in {@code plans/m3-vertical-slice.md}).
 *
 * <p>Through {@code ServerLevel.saveDebugReport}, which is public: no reflection, no access
 * transformer, no mixin.
 */
final class ChunkReport {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private ChunkReport() {
    }

    /** Writes the report under {@code debug/ascension-<time>/<namespace>/<dimension>/}; says where. */
    static Component write(MinecraftServer server) {
        Path base = server.getServerDirectory().resolve("debug").resolve("ascension-" + LocalDateTime.now().format(STAMP));
        try {
            for (ServerLevel level : server.getAllLevels()) {
                ResourceLocation id = level.dimension().location();
                Path dir = base.resolve(id.getNamespace()).resolve(id.getPath());
                Files.createDirectories(dir);
                level.saveDebugReport(dir);
            }
        } catch (IOException e) {
            return Component.literal("Could not write the debug report: " + e.getMessage());
        }
        return Component.literal("Debug report written to " + base.toAbsolutePath().normalize());
    }
}
