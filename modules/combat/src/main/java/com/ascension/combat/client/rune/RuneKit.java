package com.ascension.combat.client.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The art kit as the screen names it: the colours, and the sprite for every piece
 * (docs/technical/rune-system.md section 10). The sprites are
 * {@code textures/gui/sprites/<name>.png}, all drawn by {@code tools/generate-rune-art.py}, and
 * the colours here are that script's, written once more because Java can't read Python: a path's
 * ramp is {@code PATHS} there. Change a colour in one place and the other follows.
 *
 * <p>Growth's skill tree reuses this kit (the same night sky, brass and sockets), so what is here
 * belongs to the screens, not to the rune screen alone.
 */
public final class RuneKit {

    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_BODY = 0xFFD4D8DE;
    public static final int TEXT_DIM = 0xFF8C929C;
    public static final int TEXT_FAINT = 0xFF5C616A;
    public static final int BRASS_LOW = 0xFF7A6840;
    public static final int BRASS_MID = 0xFFC8B07A;
    public static final int BRASS_HIGH = 0xFFF0E0B0;
    public static final int XP = 0xFF9CF04C;
    public static final int BAD = 0xFFE05858;
    public static final int GOOD = 0xFF8CE03C;

    /** A path's ramp, outline to highlight, as opaque ARGB. */
    private static final Map<RunePath, int[]> RAMPS = new EnumMap<>(RunePath.class);

    static {
        RAMPS.put(RunePath.VANGUARD, new int[] {0xFF3A2208, 0xFF7C4E14, 0xFFBB8423, 0xFFEBB848, 0xFFFFF0B4});
        RAMPS.put(RunePath.REAVER, new int[] {0xFF2C080E, 0xFF6C1422, 0xFFA82636, 0xFFE2505A, 0xFFFFC4C0});
        RAMPS.put(RunePath.AETHERWRIGHT, new int[] {0xFF1C0C34, 0xFF482280, 0xFF7846C6, 0xFFA87CF0, 0xFFEAD8FF});
        RAMPS.put(RunePath.BULWARK, new int[] {0xFF0C182C, 0xFF24406C, 0xFF3C6CA8, 0xFF6EA0DC, 0xFFD4E8FF});
    }

    /** The slate a locked or undecided dial wears. */
    private static final int[] NEUTRAL = {0xFF2A2A2E, 0xFF4A4A50, 0xFF76767E, 0xFFA4A4AC, 0xFFDCDCE2};

    private RuneKit() {
    }

    /** Colour {@code index} (0 outline, 1 dark, 2 mid, 3 light, 4 highlight) of a path's ramp; none is slate. */
    public static int ramp(RunePath path, int index) {
        return (path == null ? NEUTRAL : RAMPS.get(path))[index];
    }

    /** {@code argb} with its alpha replaced. */
    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** {@code argb} with its alpha scaled, {@code factor} 0 to 1. */
    public static int fade(int argb, double factor) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0, Math.min(1.0, factor)));
        return withAlpha(argb, alpha);
    }

    public static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, name);
    }

    private static String pathName(RunePath path) {
        return path.getSerializedName();
    }

    /** A rune's own glyph, as its data file names it; dimmed when it is only open to choose. */
    public static ResourceLocation glyph(ResourceLocation icon, boolean chosen) {
        return chosen ? icon : ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), icon.getPath() + "_open");
    }

    public static ResourceLocation keystoneSocket(RunePath path, boolean world, boolean chosen) {
        return sprite("socket/" + pathName(path) + (world ? "_world_" : "_key_") + (chosen ? "chosen" : "open"));
    }

    public static ResourceLocation minorSocket(RunePath path, boolean chosen) {
        return sprite("socket/" + pathName(path) + "_minor_" + (chosen ? "chosen" : "open"));
    }

    /** A shard's moon, coloured by its stat: the stat is the icon's name, {@code shard/<stat>}. */
    public static ResourceLocation shardSocket(String stat, boolean chosen) {
        return sprite("socket/shard_" + stat + "_" + (chosen ? "chosen" : "open"));
    }

    public static ResourceLocation shardGlyph(String stat, boolean chosen) {
        return sprite("shard/" + stat + (chosen ? "" : "_open"));
    }

    /** The emblem on a path's tab: chosen, open to choose, or barred (not allowed). */
    public static ResourceLocation tabEmblem(RunePath path, String state) {
        return sprite("path/" + pathName(path) + "_small" + ("chosen".equals(state) ? "" : "_" + state));
    }

    public static ResourceLocation dial(RunePath path, boolean primary) {
        return sprite("dial/" + (primary ? "primary_" : "secondary_") + (path == null ? "none" : pathName(path)));
    }

    public static ResourceLocation card(RunePath path) {
        return sprite("card/" + (path == null ? "neutral" : pathName(path)));
    }
}
