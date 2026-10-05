package com.ascension.worlds.internal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The gatestone that rings every gateway's field. {@link #LIT} is vanilla's own {@code lit}
 * property: an attuned gate switches it on for every frame block that has one
 * ({@link GatewayFrames#setLit}), so the runes waking up is part of the activation moment the
 * design asked for. No other behaviour.
 */
public final class InscribedGatestoneBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public InscribedGatestoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
