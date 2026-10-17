package com.ascension.combat.internal.rune;

import com.ascension.combat.api.rune.RunePath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * One rune, as a data file (docs/technical/rune-system.md section 9): where it sits on a page, its
 * icon, the Java behaviour that runs it, and that behaviour's numbers. Lives in the
 * {@link RuneRegistries#RUNE} datapack registry, at
 * {@code data/<namespace>/ascension_combat/rune/<name>.json}, synced to clients so the screen can
 * show every rune and its real numbers.
 *
 * <p>The mechanic is not here: {@code effect} names one listener class (one per behaviour, as
 * every item in this module has its own {@code XyzEffects}), and that class reads this rune's
 * {@code values}. So a datapack can retune a rune, remove one, or add a variant with other numbers
 * on an existing behaviour, all without code. Adding a new behaviour is Java.
 *
 * <p>The rune's name and sentence are lang keys built from its id: {@code rune.<namespace>.<path>}
 * and {@code rune.<namespace>.<path>.desc}.
 *
 * @param path      the path a keystone or minor belongs to; shards belong to none
 * @param slot      keystone, minor or shard
 * @param row       a minor's row (0 or 1) or a shard's (0 offence, 1 flex, 2 defence); 0 for keystones
 * @param position  its place in its row or among the keystones, 0 to 2, for the screen
 * @param world     a world keystone: a behaviour for living in Minecraft rather than fighting
 * @param icon      the icon's texture, under {@code textures/gui/rune/}
 * @param effect    the behaviour that runs it
 * @param values    the behaviour's numbers, by name; times are in seconds
 * @param growthCap for a growing rune (section 4.5), how many kinds of enemy it counts at most
 */
public record RuneDefinition(
        Optional<RunePath> path,
        RuneSlot slot,
        int row,
        int position,
        boolean world,
        ResourceLocation icon,
        ResourceLocation effect,
        Map<String, Double> values,
        Optional<Integer> growthCap) {

    public static final int SHARD_ROWS = 3;
    public static final int MINOR_ROWS = 2;

    private static final Codec<RuneDefinition> UNCHECKED = RecordCodecBuilder.create(instance -> instance
            .group(
                    RunePath.CODEC.optionalFieldOf("path").forGetter(RuneDefinition::path),
                    RuneSlot.CODEC.fieldOf("slot").forGetter(RuneDefinition::slot),
                    Codec.INT.optionalFieldOf("row", 0).forGetter(RuneDefinition::row),
                    Codec.INT.optionalFieldOf("position", 0).forGetter(RuneDefinition::position),
                    Codec.BOOL.optionalFieldOf("world", false).forGetter(RuneDefinition::world),
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(RuneDefinition::icon),
                    ResourceLocation.CODEC.fieldOf("effect").forGetter(RuneDefinition::effect),
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("values", Map.of())
                            .forGetter(RuneDefinition::values),
                    Codec.INT.optionalFieldOf("growth_cap").forGetter(RuneDefinition::growthCap))
            .apply(instance, RuneDefinition::new));

    /**
     * Checked in the codec, so a bad file is reported by name instead of crashing the datapack
     * load (the lesson {@code WorldEnvironment}'s codec records).
     */
    public static final Codec<RuneDefinition> CODEC = UNCHECKED.validate(RuneDefinition::check);

    private static DataResult<RuneDefinition> check(RuneDefinition rune) {
        if (rune.slot != RuneSlot.SHARD && rune.path.isEmpty()) {
            return DataResult.error(() -> "A " + rune.slot.getSerializedName() + " needs a path");
        }
        if (rune.slot == RuneSlot.SHARD && rune.path.isPresent()) {
            return DataResult.error(() -> "A shard belongs to no path");
        }
        int rows = rune.slot == RuneSlot.MINOR ? MINOR_ROWS : rune.slot == RuneSlot.SHARD ? SHARD_ROWS : 1;
        if (rune.row < 0 || rune.row >= rows) {
            return DataResult.error(() -> "row " + rune.row + " is out of range for a " + rune.slot.getSerializedName());
        }
        if (rune.position < 0 || rune.position > 2) {
            return DataResult.error(() -> "position must be 0, 1 or 2, got " + rune.position);
        }
        if (rune.world && rune.slot != RuneSlot.KEYSTONE) {
            return DataResult.error(() -> "Only a keystone can be a world keystone");
        }
        if (rune.growthCap.isPresent() && rune.growthCap.get() <= 0) {
            return DataResult.error(() -> "growth_cap must be positive");
        }
        return DataResult.success(rune);
    }

    /** A number from {@code values}, or {@code fallback} when the file leaves it out. */
    public double value(String key, double fallback) {
        return values.getOrDefault(key, fallback);
    }

    /** A count from {@code values}, rounded. */
    public int count(String key, int fallback) {
        return (int) Math.round(value(key, fallback));
    }

    /** A time from {@code values}, written in seconds, as game ticks. */
    public long ticks(String key, double fallbackSeconds) {
        return Math.round(value(key, fallbackSeconds) * 20.0);
    }

    public boolean grows() {
        return growthCap.isPresent();
    }
}
