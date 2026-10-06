package com.ascension.worlds.internal.archive;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One tile of the Lunar Archive's carved murals. Each mural is a picture several blocks wide, cut
 * into 16-pixel tiles, and {@link #TILE} says which tile this block shows: tile 0 is bare plaster,
 * the rest are numbered mural by mural in the order {@code tools/generate-archive-art.py} lists
 * them. The structure template sets the tile; nothing changes it in play. Decoration only, no
 * behaviour. Design record: {@code docs/technical/lunar-archive-design.md}.
 *
 * <p>Mined, a tile drops itself with its tile kept ({@code minecraft:copy_state} in its loot table),
 * so a mural taken down and put back up still shows its picture.
 */
public final class ArchiveMuralBlock extends Block {

    /** Must equal {@code MURAL_TILES} in {@code tools/generate-archive-art.py}, which checks it. */
    public static final int TILES = 121;

    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, TILES - 1);

    public ArchiveMuralBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TILE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TILE);
    }
}
