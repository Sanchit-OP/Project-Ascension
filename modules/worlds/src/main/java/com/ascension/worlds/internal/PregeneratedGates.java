package com.ascension.worlds.internal;

import com.ascension.worlds.internal.gateway.GatewayFrame;
import com.ascension.worlds.internal.worldgen.GatewayFramePiece;
import com.ascension.worlds.internal.worldgen.GatewayFrameStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

/**
 * The gates worldgen builds on a planet (the Moon's, the Nether's) cannot be broken (Sanchit,
 * 2026-10-11). A player-built gate on Earth can.
 *
 * <p>Why: the Nether's ruin is built of gold, and a newly arrived player needs gold to survive the
 * rot. A gate stripped for its gold would fail the check attunement makes, and the Monstrosity's
 * key would light nothing until it was rebuilt. The world's one gate on a planet is not a quarry.
 *
 * <p>Only the frame's own blocks (keystone included, the opening not); the ruin's decoration around
 * it can be mined. Mining, explosions (ghast fireballs, creepers), pistons and block-breaking mobs
 * (the Wither) are all stopped; creative players can still break it, for building and debugging.
 *
 * <p>Event-driven: each check asks the level's structure manager whether a gate's piece covers the
 * position (the structure references of one chunk), and only then compares against the frame's
 * cells. Nothing is held between events.
 */
public final class PregeneratedGates {

    private static final Component UNBREAKABLE = Component.translatable("ascension_worlds.gateway.unbreakable");

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PregeneratedGates::onBreak);
        NeoForge.EVENT_BUS.addListener(PregeneratedGates::onDetonate);
        NeoForge.EVENT_BUS.addListener(PregeneratedGates::onPiston);
        NeoForge.EVENT_BUS.addListener(PregeneratedGates::onMobBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getPlayer().isCreative()
                && isFrameBlock(level, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(UNBREAKABLE, true);
        }
    }

    private static void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(pos -> isFrameBlock(level, pos));
        }
    }

    private static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PistonStructureResolver moving = event.getStructureHelper();
        if (moving == null || !moving.resolve()) {
            return;
        }
        if (moving.getToPush().stream().anyMatch(pos -> isFrameBlock(level, pos))
                || moving.getToDestroy().stream().anyMatch(pos -> isFrameBlock(level, pos))) {
            event.setCanceled(true);
        }
    }

    private static void onMobBreak(LivingDestroyBlockEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && isFrameBlock(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Whether a block of a pre-generated gate's frame stands here. */
    static boolean isFrameBlock(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(
                pos, holder -> holder.value() instanceof GatewayFrameStructure);
        if (!start.isValid()) {
            return false;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof GatewayFramePiece framePiece && piece.getBoundingBox().isInside(pos)) {
                Optional<GatewayFrame> frame = GatewayFrames.get(level.registryAccess(), framePiece.frame());
                if (frame.isPresent() && framePiece.placement(frame.get()).isFrameBlock(pos.getX(), pos.getY(), pos.getZ())) {
                    return true;
                }
            }
        }
        return false;
    }

    private PregeneratedGates() {
    }
}
