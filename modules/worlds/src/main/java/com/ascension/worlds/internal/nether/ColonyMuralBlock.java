package com.ascension.worlds.internal.nether;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One tile of the Nether colonists' own carving: gold inlaid in their black stone, the reverse of
 * the Sealers' pale plaster. {@link #TILE} says which tile this block shows: tile 0 is the bare
 * stone, the rest are numbered picture by picture in the order {@code tools/generate-nether-art.py}
 * lists them. The structure template sets the tile; nothing changes it in play. Decoration only, no
 * behaviour. Design record: {@code docs/technical/nether-art-design.md}.
 *
 * <p>Mined, a tile drops itself with its tile kept ({@code minecraft:copy_state}), so a picture
 * taken down and put back up still shows.
 */
public final class ColonyMuralBlock extends Block {

    /** Must equal {@code COLONY_TILES} in {@code tools/generate-nether-art.py}, which checks it. */
    public static final int TILES = 11;

    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, TILES - 1);

    public ColonyMuralBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TILE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TILE);
    }
}
