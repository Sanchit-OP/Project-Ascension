package com.ascension.worlds.internal.archive;

import net.minecraft.world.level.block.Block;

/**
 * The base of the Archive's star map. For now an ordinary block, so the Archive's design session
 * can place it in its templates; the hologram of the solar system that floats above it, with every
 * planet's gate shown dark or lit, comes in the next increment
 * ({@code docs/technical/lunar-archive-format.md}).
 *
 * <p>Unbreakable in survival for the same reason as {@link ArchivePlinthBlock}.
 */
public final class ArchiveOrreryBlock extends Block {

    public ArchiveOrreryBlock(Properties properties) {
        super(properties);
    }
}
