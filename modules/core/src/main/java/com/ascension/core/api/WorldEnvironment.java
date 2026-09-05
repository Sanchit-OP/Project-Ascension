package com.ascension.core.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a whole world is like to be in.
 *
 * <p>The shared vocabulary for "this dimension is airless" (ADR-0011). One module knows it &mdash;
 * whoever owns the world &mdash; and another cares about it, and neither has to depend on the
 * other to agree.
 *
 * <p><strong>This is not an atmosphere at a position.</strong> {@code ascension-atmosphere} has
 * its own {@code Atmosphere} record for that, which is what a sealed room, a body of water and a
 * conduit all answer with. The two happen to carry the same two fields today, and conflating them
 * would say that a pressurised room and a planet are the same kind of thing &mdash; which is
 * exactly the confusion that module's priority bands exist to prevent. A world's environment is
 * the <em>baseline</em> a position-level answer can override.
 *
 * <p>Deliberately two fields, for the reason stated in {@code docs/gameplay/oxygen.md}: this is
 * authored gameplay logic, not a gas simulation. A thin atmosphere drains at {@code 0.5}, a
 * corrosive one at {@code 2.0}, and that is the whole model.
 *
 * @param breathable      whether a player can breathe here with no supply of their own
 * @param drainMultiplier how fast a supply is consumed here; meaningless when {@code breathable}
 */
public record WorldEnvironment(boolean breathable, float drainMultiplier) {

    /** Ordinary breathable air. Earth, and the default for anything unclaimed. */
    public static final WorldEnvironment EARTHLIKE = new WorldEnvironment(true, 0.0f);

    /** No air at all, draining at the baseline rate. The Moon. */
    public static final WorldEnvironment AIRLESS = new WorldEnvironment(false, 1.0f);

    /**
     * Serialised form, for authoring a world's environment as data.
     *
     * <p>Both fields optional, defaulting to {@link #EARTHLIKE}, so {@code {}} is a valid and
     * meaningful environment. Lives here rather than in a consumer because the point of
     * ADR-0011 is that the <em>format</em> is shared, not just the type.
     *
     * <p><strong>The drain field is validated in the codec, not left to the constructor.</strong>
     * The compact constructor throws, which is right for a Java caller passing nonsense — fail
     * fast on a programmer error. It is wrong for a datapack: an exception escaping a codec
     * aborts datapack loading instead of reporting which file was bad. Validating here turns a
     * hostile value into a {@code DataResult} error that names itself.
     *
     * <p>A unit test caught this. The constructor was doing the checking, the codec surfaced it
     * as a crash, and nothing in a compile or an in-game smoke test would have revealed it until
     * somebody authored a bad planet.
     */
    private static final Codec<Float> DRAIN_CODEC = Codec.FLOAT.validate(value ->
            value >= 0.0f && Float.isFinite(value)
                    ? DataResult.success(value)
                    : DataResult.error(() ->
                            "drain_multiplier must be finite and non-negative, got " + value));

    public static final Codec<WorldEnvironment> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.optionalFieldOf("breathable", Boolean.TRUE)
                            .forGetter(WorldEnvironment::breathable),
                    DRAIN_CODEC.optionalFieldOf("drain_multiplier", 0.0f)
                            .forGetter(WorldEnvironment::drainMultiplier))
            .apply(instance, WorldEnvironment::new));

    /** For syncing to clients, which need it to know what a world will do to them. */
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldEnvironment> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, WorldEnvironment::breathable,
                    ByteBufCodecs.FLOAT, WorldEnvironment::drainMultiplier,
                    WorldEnvironment::new);

    public WorldEnvironment {
        if (drainMultiplier < 0.0f || !Float.isFinite(drainMultiplier)) {
            throw new IllegalArgumentException(
                    "drainMultiplier must be finite and non-negative, got " + drainMultiplier);
        }
    }

    /** Unbreathable air draining at the given rate. */
    public static WorldEnvironment hostile(float drainMultiplier) {
        return new WorldEnvironment(false, drainMultiplier);
    }
}
