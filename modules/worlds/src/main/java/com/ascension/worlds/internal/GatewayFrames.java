package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.internal.gateway.GatewayFrame;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Where {@link GatewayFrame}s live, and the bridge between their pure geometry and real blocks:
 * what counts as a match for a key cell, and how a frame is written into a level.
 *
 * <p>A <strong>datapack registry</strong>, {@code data/<ns>/ascension_worlds/gateway_frame/<id>.json},
 * like planets &mdash; the frame is authored (by the design session), not coded. Server-only: no
 * client ever needs the pattern.
 */
public final class GatewayFrames {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayFrames.class);

    public static final ResourceKey<Registry<GatewayFrame>> REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "gateway_frame"));

    /** The one frame every gateway uses today. Per-planet frames would be a field on the planet. */
    public static final ResourceKey<GatewayFrame> DEFAULT = ResourceKey.create(
            REGISTRY, ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "gateway"));

    private GatewayFrames() {
    }

    public static Optional<GatewayFrame> get(RegistryAccess access, ResourceKey<GatewayFrame> id) {
        return access.registry(REGISTRY).flatMap(registry -> registry.getOptional(id));
    }

    public static Optional<GatewayFrame> get(RegistryAccess access) {
        return get(access, DEFAULT);
    }

    /** What the opening may hold when an unlit frame is checked: nothing in the way. */
    public enum Opening {
        /** Activation: every field cell must be clear. */
        CLEAR,
        /** Re-lighting an attuned frame: clear, or already holding field blocks. */
        CLEAR_OR_FIELD
    }

    /** A {@link GatewayFrame.CellTest} that reads real blocks from {@code level}. */
    public static GatewayFrame.CellTest testIn(LevelAccessor level, GatewayFrame frame, Opening opening) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        return (symbol, x, y, z) -> {
            BlockState state = level.getBlockState(cursor.set(x, y, z));
            if (symbol == frame.fieldSymbol()) {
                return state.canBeReplaced()
                        || (opening == Opening.CLEAR_OR_FIELD && state.is(WorldsContent.GATEWAY_FIELD.get()));
            }
            if (symbol == frame.keystoneSymbol()) {
                return state.is(WorldsContent.GATEWAY_KEYSTONE.get());
            }
            return matches(state, frame.key().get(symbol));
        };
    }

    static boolean matches(BlockState state, GatewayFrame.Key key) {
        if (key.tag().isPresent()) {
            return state.is(TagKey.create(Registries.BLOCK, key.tag().get()));
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(key.block().orElseThrow());
    }

    /**
     * Writes {@code placement} into {@code level}: every frame cell its placed block, the keystone
     * (attuned or not), and the opening as field blocks if {@code lit}, else air. Cells outside
     * {@code clip} are skipped, which is what lets worldgen write a frame spanning several chunks
     * one chunk at a time; pass null to write everything.
     */
    public static void write(LevelAccessor level, GatewayFrame frame, GatewayFrame.Placement placement,
                             boolean lit, BoundingBox clip) {
        BlockState field = fieldState(placement);
        BlockState keystone = WorldsContent.GATEWAY_KEYSTONE.get().defaultBlockState()
                .setValue(GatewayKeystoneBlock.ACTIVE, lit);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (GatewayFrame.Cell cell : frame.cells()) {
            int[] at = placement.world(cell.x(), cell.y(), cell.z());
            cursor.set(at[0], at[1], at[2]);
            if (clip != null && !clip.isInside(cursor)) {
                continue;
            }
            BlockState state;
            if (cell.symbol() == frame.fieldSymbol()) {
                state = lit ? field : Blocks.AIR.defaultBlockState();
            } else if (cell.symbol() == frame.keystoneSymbol()) {
                state = keystone;
            } else {
                state = placedState(frame.key().get(cell.symbol()));
            }
            level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Fill an already-standing frame's opening with field blocks. */
    public static void light(LevelAccessor level, GatewayFrame.Placement placement) {
        BlockState field = fieldState(placement);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int[] at : placement.fieldPositions()) {
            level.setBlock(cursor.set(at[0], at[1], at[2]), field, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState fieldState(GatewayFrame.Placement placement) {
        return WorldsContent.GATEWAY_FIELD.get().defaultBlockState()
                .setValue(GatewayFieldBlock.AXIS, placement.facing().fieldAlongX() ? Direction.Axis.X : Direction.Axis.Z);
    }

    public static BlockState placedState(GatewayFrame.Key key) {
        ResourceLocation id = key.placed();
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty()) {
            LOGGER.error("Gateway frame names block {}, which does not exist; placing nothing there", id);
            return Blocks.AIR.defaultBlockState();
        }
        return block.get().defaultBlockState();
    }
}
