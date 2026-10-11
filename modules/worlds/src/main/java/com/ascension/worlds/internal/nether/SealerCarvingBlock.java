package com.ascension.worlds.internal.nether;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One tile of the Sealers' carving in the Nether's soul sand valleys, cut in their pale plaster the
 * way the Moon's murals are. {@link #TILE} says which tile this block shows: tile 0 is bare
 * plaster, the rest are numbered picture by picture in the order {@code tools/generate-nether-art.py}
 * lists them. The structure template sets the tile; nothing changes it in play. Decoration only, no
 * behaviour. Design record: {@code docs/technical/nether-art-design.md}.
 *
 * <p>Mined, a tile drops itself with its tile kept ({@code minecraft:copy_state}).
 */
public final class SealerCarvingBlock extends Block {

    /** Must equal {@code CARVING_TILES} in {@code tools/generate-nether-art.py}, which checks it. */
    public static final int TILES = 15;

    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, TILES - 1);

    public SealerCarvingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TILE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TILE);
    }
}
