package com.ascension.compat.ftbquests.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Where everything is on the Planets page's picture, as written by {@code tools/generate-quests.py}
 * next to the picture itself ({@code assets/ascension_compat_ftbquests/quests/chart_layout.json}).
 *
 * <p>The tool lays the page out from the planet files and draws the picture from the same numbers, so
 * this file is the one place the client learns them: nothing is copied by hand, and nothing is asked of
 * the server. The projection is plain arithmetic ({@link ChartMath}); a test replays the sample points the
 * tool wrote against it, so the two cannot drift apart unseen.
 *
 * @param size          the picture is {@code size} x {@code size} pixels
 * @param centerX       where Earth, the origin of space, is on the picture, in pixels
 * @param centerY       same, vertically
 * @param radius        pixels from the centre to the edge of the chart
 * @param rangeBlocks   the distance in blocks that lands on the edge
 * @param unitsAcross   how wide the picture is in the quest book's own units (for the tool's layout only)
 * @param space         the dimension in which a player's own position is plotted
 * @param planets       each world a player can stand on, with where it sits on the picture
 */
public record ChartLayout(
        int size,
        double centerX,
        double centerY,
        double radius,
        double rangeBlocks,
        double unitsAcross,
        ResourceLocation space,
        List<Planet> planets) {

    /** {@code [a, b]} as two doubles; its own class so neither record's static set-up waits on the other. */
    private static final class Pairs {
        static final Codec<List<Double>> PAIR = Codec.DOUBLE.listOf().validate(list ->
                list.size() == 2
                        ? DataResult.success(list)
                        : DataResult.error(() -> "expected [a, b], got " + list));

        private Pairs() {
        }
    }

    /**
     * One world on the chart.
     *
     * @param surface the dimension a player is in while standing on it
     * @param blocksX its position in space, in blocks
     * @param blocksZ same, along Z
     * @param pixelX  where it is on the picture
     * @param pixelY  same, vertically
     */
    public record Planet(ResourceLocation surface, double blocksX, double blocksZ, double pixelX, double pixelY) {

        static final Codec<Planet> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("surface").forGetter(Planet::surface),
                Pairs.PAIR.fieldOf("position").forGetter(p -> List.of(p.blocksX(), p.blocksZ())),
                Pairs.PAIR.fieldOf("pixel").forGetter(p -> List.of(p.pixelX(), p.pixelY()))
        ).apply(i, (surface, position, pixel) ->
                new Planet(surface, position.get(0), position.get(1), pixel.get(0), pixel.get(1))));
    }

    public static final Codec<ChartLayout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("size").forGetter(ChartLayout::size),
            Pairs.PAIR.fieldOf("center").forGetter(l -> List.of(l.centerX(), l.centerY())),
            Codec.DOUBLE.fieldOf("radius").forGetter(ChartLayout::radius),
            Codec.DOUBLE.fieldOf("range_blocks").forGetter(ChartLayout::rangeBlocks),
            Codec.DOUBLE.fieldOf("units_across").forGetter(ChartLayout::unitsAcross),
            ResourceLocation.CODEC.fieldOf("space").forGetter(ChartLayout::space),
            Planet.CODEC.listOf().fieldOf("planets").forGetter(ChartLayout::planets)
    ).apply(i, (size, center, radius, range, units, space, planets) ->
            new ChartLayout(size, center.get(0), center.get(1), radius, range, units, space, List.copyOf(planets))));
}
