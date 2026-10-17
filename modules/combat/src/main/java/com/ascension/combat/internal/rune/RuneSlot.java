package com.ascension.combat.internal.rune;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Where on a page a rune goes (docs/technical/rune-system.md section 3).
 *
 * <ul>
 *   <li>{@code KEYSTONE}: one of a path's three, the primary path's only. One may be a world
 *       keystone ({@link RuneDefinition#world()}).</li>
 *   <li>{@code MINOR}: one of a path's two rows of three; a page takes one per row, on either
 *       side.</li>
 *   <li>{@code SHARD}: a stat shard, in one of three rows (offence, flex, defence), belonging to no
 *       path.</li>
 * </ul>
 */
public enum RuneSlot implements StringRepresentable {
    KEYSTONE,
    MINOR,
    SHARD;

    public static final Codec<RuneSlot> CODEC = StringRepresentable.fromEnum(RuneSlot::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
