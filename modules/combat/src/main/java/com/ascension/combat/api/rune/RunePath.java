package com.ascension.combat.api.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * The four rune paths (docs/technical/rune-system.md section 4). A public type: unlike a specific
 * rune's mechanic, which player has picked which path is exactly the kind of thing another mod
 * might reasonably want to read (a third-party balance tweak keyed on path, say), so this lives in
 * {@code api} rather than {@code internal}.
 *
 * <p>Only four, not League's five &mdash; Inspiration dropped entirely rather than kept as a
 * weaker fifth, Sanchit's call recorded in the design doc, not an oversight here.
 *
 * <p>Written lower case ({@code "vanguard"}), as rune data files name it. Read in either case,
 * because rune pages saved before 2026-10-17 wrote the enum's own upper-case name.
 */
public enum RunePath implements StringRepresentable {
    VANGUARD,
    REAVER,
    AETHERWRIGHT,
    BULWARK;

    public static final Codec<RunePath> CODEC = Codec.STRING.comapFlatMap(RunePath::parse, RunePath::getSerializedName);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<RunePath> parse(String name) {
        for (RunePath path : values()) {
            if (path.name().equalsIgnoreCase(name)) {
                return DataResult.success(path);
            }
        }
        return DataResult.error(() -> "Unknown rune path '" + name + "'; expected vanguard, reaver, aetherwright or bulwark");
    }
}
