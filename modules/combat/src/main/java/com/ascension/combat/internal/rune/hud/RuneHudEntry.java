package com.ascension.combat.internal.rune.hud;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What one equipped rune is doing right now, as the HUD needs it (docs/technical/hud.md section 4).
 *
 * <p>State only, never a timer: anything that counts down carries the game tick it ends at, and the
 * client animates the rest by itself. So an entry does not change while a cooldown runs, and the
 * server only sends when an entry actually changes (ADR-0007: never a packet per tick).
 *
 * @param rune    the rune's effect id; its glyph is {@code <namespace>:rune/<path>}
 * @param path    the rune's path, as a {@code RunePath} ordinal, for its colour
 * @param kind    what {@code value} and {@code endTick} mean
 * @param value   STACKS: stacks held; CHARGE: hits counted; WINDOW: hits left when it counts them
 * @param max     STACKS and CHARGE: the most there can be; otherwise 0
 * @param endTick game tick it ends or becomes ready; 0 when it does not
 */
public record RuneHudEntry(ResourceLocation rune, int path, Kind kind, int value, int max, long endTick) {

    /** What an entry measures. */
    public enum Kind {
        /** A count that builds: Conqueror's Resolve, Relentless Strikes, Momentum. */
        STACKS,
        /** A run toward a payoff: Cadence's hits, Arc's hits. */
        CHARGE,
        /** Time until it can fire again: Second Wind, Starfall, Brace. */
        COOLDOWN,
        /** A window that is open for a while: Drop In, Riposte, Plating, Vanish. */
        WINDOW
    }

    public static final StreamCodec<ByteBuf, RuneHudEntry> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RuneHudEntry::rune,
            ByteBufCodecs.VAR_INT, RuneHudEntry::path,
            ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), RuneHudEntry::kind,
            ByteBufCodecs.VAR_INT, RuneHudEntry::value,
            ByteBufCodecs.VAR_INT, RuneHudEntry::max,
            ByteBufCodecs.VAR_LONG, RuneHudEntry::endTick,
            RuneHudEntry::new);
}
