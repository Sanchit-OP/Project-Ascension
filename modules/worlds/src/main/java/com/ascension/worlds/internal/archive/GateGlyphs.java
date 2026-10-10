package com.ascension.worlds.internal.archive;

import com.ascension.worlds.internal.GateProgress;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Lights the carved glyphs ({@link MarkGlyphBlock}) to match the gates: glyph <em>k</em> glows once
 * gate <em>k</em> is lit ({@code docs/technical/lore-art-format.md}, section 5). On the Archive's
 * pylon that is the Sealers' warning counting the broken locks, and the light seen from Earth
 * ({@code docs/lore/the-mark.md}).
 *
 * <p><strong>No scanning.</strong> A chunk that loads is checked section by section against its
 * block palette, the short list of states a section holds, so a section without a glyph costs one
 * palette lookup and is never walked. The glyphs found are remembered by position until the chunk
 * unloads. When the gates change, {@link #refresh} visits only those; when a chunk with glyphs
 * loads, it is brought up to date on the next server task, never inside the load itself.
 *
 * <p><strong>Any glyph, anywhere.</strong> Found by block, not by structure, so a planet's ruins can
 * carry their own glyph over their gate (the-mark.md) with no new Java, and a glyph hand-placed in
 * creative joins in the next time its chunk loads.
 *
 * <p>Positions and dimension keys only, server thread only, emptied when a level unloads and when
 * the server stops (ADR-0007).
 */
public final class GateGlyphs {

    /** Glyph positions ({@link BlockPos#asLong}) in each loaded chunk that has any, per dimension. */
    private static final Map<ResourceKey<Level>, Long2ObjectMap<long[]>> LOADED = new HashMap<>();

    private GateGlyphs() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GateGlyphs::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(GateGlyphs::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(GateGlyphs::onLevelUnload);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> LOADED.clear());
    }

    /** Brings every loaded glyph up to date with the gates. Called when the network changes. */
    public static void refresh(MinecraftServer server) {
        int stage = GateProgress.markStage(server);
        LOADED.forEach((dimension, chunks) -> {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                chunks.values().forEach(glyphs -> apply(level, glyphs, stage));
            }
        });
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        long[] glyphs = find(chunk);
        if (glyphs.length == 0) {
            return;
        }
        LOADED.computeIfAbsent(level.dimension(), dimension -> new Long2ObjectOpenHashMap<>())
                .put(chunk.getPos().toLong(), glyphs);
        // Not during the load itself: changing blocks while a chunk is being finished is unsafe.
        level.getServer().execute(() -> apply(level, glyphs, GateProgress.markStage(level.getServer())));
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Long2ObjectMap<long[]> chunks = LOADED.get(level.dimension());
            if (chunks != null && chunks.remove(event.getChunk().getPos().toLong()) != null && chunks.isEmpty()) {
                LOADED.remove(level.dimension());
            }
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.remove(level.dimension());
        }
    }

    /** Every glyph in the chunk. Sections whose palette has no glyph are skipped unread. */
    private static long[] find(LevelChunk chunk) {
        Block glyph = ArchiveDecorBlocks.MARK_GLYPH.get();
        LongArrayList found = new LongArrayList();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.getStates().maybeHas(state -> state.is(glyph))) {
                continue;
            }
            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).is(glyph)) {
                            found.add(BlockPos.asLong(chunk.getPos().getMinBlockX() + x, bottomY + y,
                                    chunk.getPos().getMinBlockZ() + z));
                        }
                    }
                }
            }
        }
        return found.toLongArray();
    }

    private static void apply(ServerLevel level, long[] glyphs, int stage) {
        for (long packed : glyphs) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ArchiveDecorBlocks.MARK_GLYPH.get())) {
                continue;
            }
            boolean lit = GateProgress.glyphLit(state.getValue(MarkGlyphBlock.GLYPH), stage);
            if (state.getValue(MarkGlyphBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(MarkGlyphBlock.LIT, lit), Block.UPDATE_CLIENTS);
            }
        }
    }
}
