package com.ascension.worlds.internal.archive;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One glyph of the script, cut into anorthosite: the band of seven down the standing pylon over the
 * Lunar Archive. {@link #GLYPH} is which of the seven it is, in gate order (0 Earth, 1 the Moon, 2 the
 * burning world, 3 to 6 the later worlds); {@link #LIT} is whether that gate has been lit. Unlit it is
 * a dark carving; lit, the cut glows teal and the block gives light 7.
 *
 * <p>Decoration only, with no behaviour of its own: the structure template places all seven unlit,
 * and logic sets {@code lit} when a gate is attuned, finding the blocks by id and by these two
 * property names, so neither side depends on the other's classes
 * ({@code docs/technical/lore-art-format.md}, section 5). Design record:
 * {@code docs/technical/lore-art-design.md}.
 */
public final class MarkGlyphBlock extends Block {

    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 6);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MarkGlyphBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GLYPH, 0).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLYPH, LIT);
    }
}
