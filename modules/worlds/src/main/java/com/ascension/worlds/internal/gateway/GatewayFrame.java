package com.ascension.worlds.internal.gateway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A gateway portal's frame, as authored in {@code data/<ns>/ascension_worlds/gateway_frame/<id>.json}
 * &mdash; the format and every rule below are specified in
 * {@code docs/technical/gateway-frame-format.md}, the contract between the design session (which
 * writes the JSON) and this code (which reads it).
 *
 * <p>Pure geometry: characters in a grid, no {@code Level} and no {@code BlockState}. What a key
 * character actually <em>means</em> in a world is answered by whoever calls {@link #match} or
 * {@link Placement#cells} &mdash; which is what makes rotation and matching unit-testable.
 *
 * <p>Local coordinates: {@code x} left to right, {@code y} bottom to top, {@code z} front
 * ({@code z = 0}) to back. {@link Facing} maps them into the world.
 */
public final class GatewayFrame {

    /** One key character's meaning: an exact block, or any block in a tag plus what to place. */
    public record Key(Optional<ResourceLocation> block, Optional<ResourceLocation> tag, Optional<ResourceLocation> place) {

        static final Codec<Key> CODEC = RecordCodecBuilder.<Key>create(instance -> instance
                        .group(
                                ResourceLocation.CODEC.optionalFieldOf("block").forGetter(Key::block),
                                ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(Key::tag),
                                ResourceLocation.CODEC.optionalFieldOf("place").forGetter(Key::place))
                        .apply(instance, Key::new))
                .validate(Key::check);

        private static DataResult<Key> check(Key key) {
            if (key.block.isPresent() == key.tag.isPresent()) {
                return DataResult.error(() -> "a key entry needs exactly one of \"block\" or \"tag\"");
            }
            if (key.tag.isPresent() && key.place.isEmpty()) {
                return DataResult.error(() -> "a \"tag\" key entry also needs \"place\" (what pre-generation puts there)");
            }
            return DataResult.success(key);
        }

        /** What pre-generation places for this cell. */
        public ResourceLocation placed() {
            return block.orElseGet(place::orElseThrow);
        }
    }

    /** A cell of the pattern that is not a space. */
    public record Cell(int x, int y, int z, char symbol) {
    }

    static final int MAX_WIDTH = 15;
    static final int MAX_HEIGHT = 20;
    static final int MAX_DEPTH = 9;
    static final int MIN_FIELD_WIDTH = 2;
    static final int MIN_FIELD_HEIGHT = 3;

    private record Raw(Map<String, Key> key, String keystone, String field, List<List<String>> layers) {
    }

    public static final Codec<GatewayFrame> CODEC = RecordCodecBuilder.<Raw>create(instance -> instance
                    .group(
                            Codec.unboundedMap(Codec.STRING, Key.CODEC).fieldOf("key").forGetter(Raw::key),
                            Codec.STRING.fieldOf("keystone").forGetter(Raw::keystone),
                            Codec.STRING.fieldOf("field").forGetter(Raw::field),
                            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(Raw::layers))
                    .apply(instance, Raw::new))
            .flatXmap(GatewayFrame::build, frame -> DataResult.success(frame.raw));

    private final Raw raw;
    private final Map<Character, Key> key;
    private final char keystoneSymbol;
    private final char fieldSymbol;
    private final List<Cell> cells;
    private final Cell keystone;
    private final int fieldMinX;
    private final int fieldMaxX;
    private final int fieldMinY;
    private final int fieldMaxY;
    private final int fieldZ;

    private GatewayFrame(Raw raw, Map<Character, Key> key, char keystoneSymbol, char fieldSymbol, List<Cell> cells,
                         Cell keystone, int fieldMinX, int fieldMaxX, int fieldMinY, int fieldMaxY, int fieldZ) {
        this.raw = raw;
        this.key = key;
        this.keystoneSymbol = keystoneSymbol;
        this.fieldSymbol = fieldSymbol;
        this.cells = cells;
        this.keystone = keystone;
        this.fieldMinX = fieldMinX;
        this.fieldMaxX = fieldMaxX;
        this.fieldMinY = fieldMinY;
        this.fieldMaxY = fieldMaxY;
        this.fieldZ = fieldZ;
    }

    /** Parses and checks every rule in the contract; any violation is a load error naming it. */
    static DataResult<GatewayFrame> build(Raw raw) {
        if (raw.keystone.length() != 1 || raw.field.length() != 1) {
            return DataResult.error(() -> "\"keystone\" and \"field\" must each be a single character");
        }
        char keystoneSymbol = raw.keystone.charAt(0);
        char fieldSymbol = raw.field.charAt(0);

        DataResult<Map<Character, Key>> parsedKey = PatternGrid.parseKey(raw.key, "");
        if (parsedKey.error().isPresent()) {
            return parsedKey.map(k -> null);
        }
        Map<Character, Key> key = parsedKey.getOrThrow();
        if (key.containsKey(fieldSymbol)) {
            return DataResult.error(() -> "the field character '" + fieldSymbol + "' must not also be in \"key\"");
        }
        if (!key.containsKey(keystoneSymbol)) {
            return DataResult.error(() -> "the keystone character '" + keystoneSymbol + "' must be in \"key\"");
        }

        DataResult<List<Cell>> parsedCells = PatternGrid.parseLayers(raw.layers, MAX_WIDTH, MAX_HEIGHT, MAX_DEPTH,
                symbol -> symbol == fieldSymbol || key.containsKey(symbol), "");
        if (parsedCells.error().isPresent()) {
            return parsedCells.map(c -> null);
        }
        List<Cell> cells = parsedCells.getOrThrow();

        List<Cell> keystones = cells.stream().filter(c -> c.symbol == keystoneSymbol).toList();
        if (keystones.size() != 1) {
            return DataResult.error(() -> "need exactly one keystone cell, found " + keystones.size());
        }
        List<Cell> field = cells.stream().filter(c -> c.symbol == fieldSymbol).toList();
        if (field.isEmpty()) {
            return DataResult.error(() -> "no field cells");
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int fieldZ = field.get(0).z;
        for (Cell cell : field) {
            if (cell.z != fieldZ) {
                return DataResult.error(() -> "field cells must all be in one row (one z): found z=" + fieldZ + " and z=" + cell.z);
            }
            minX = Math.min(minX, cell.x);
            maxX = Math.max(maxX, cell.x);
            minY = Math.min(minY, cell.y);
            maxY = Math.max(maxY, cell.y);
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        if (field.size() != width * height) {
            return DataResult.error(() -> "field cells must form one solid rectangle");
        }
        if (width < MIN_FIELD_WIDTH || height < MIN_FIELD_HEIGHT) {
            return DataResult.error(() -> "field must be at least " + MIN_FIELD_WIDTH + " wide and " + MIN_FIELD_HEIGHT
                    + " tall, got " + width + "x" + height);
        }

        GatewayFrame frame = new GatewayFrame(raw, key, keystoneSymbol, fieldSymbol, cells,
                keystones.get(0), minX, maxX, minY, maxY, fieldZ);

        for (int x = minX; x <= maxX; x++) {
            if (!frame.isKeyCell(x, minY - 1, fieldZ) || !frame.isKeyCell(x, maxY + 1, fieldZ)) {
                return DataResult.error(() -> "field is not enclosed above and below by frame blocks");
            }
        }
        for (int y = minY; y <= maxY; y++) {
            if (!frame.isKeyCell(minX - 1, y, fieldZ) || !frame.isKeyCell(maxX + 1, y, fieldZ)) {
                return DataResult.error(() -> "field is not enclosed left and right by frame blocks");
            }
        }
        return DataResult.success(frame);
    }

    private boolean isKeyCell(int x, int y, int z) {
        for (Cell cell : cells) {
            if (cell.x == x && cell.y == y && cell.z == z) {
                return key.containsKey(cell.symbol);
            }
        }
        return false;
    }

    public Map<Character, Key> key() {
        return key;
    }

    public char fieldSymbol() {
        return fieldSymbol;
    }

    public char keystoneSymbol() {
        return keystoneSymbol;
    }

    /** Every non-space cell, frame and field alike. */
    public List<Cell> cells() {
        return cells;
    }

    public Cell keystone() {
        return keystone;
    }

    /** What a {@link #match} asks of the world: does the block at this position satisfy this cell? */
    @FunctionalInterface
    public interface CellTest {

        /**
         * @param symbol a key character, or {@link #fieldSymbol()} (the caller decides what an
         *               acceptable unlit opening looks like &mdash; usually air)
         */
        boolean matches(char symbol, int x, int y, int z);
    }

    /**
     * Finds how this frame sits around a keystone at {@code (kx, ky, kz)}, trying each of the four
     * facings, or empty if the blocks there match none of them. Cost is bounded by the pattern:
     * at most four passes over its cells, run once per activation click.
     */
    public Optional<Placement> match(int kx, int ky, int kz, CellTest test) {
        for (Facing facing : Facing.values()) {
            Placement candidate = placeKeystoneAt(kx, ky, kz, facing);
            if (candidate.matches(test)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** This frame laid out so that its keystone lands on {@code (kx, ky, kz)}, facing {@code facing}. */
    public Placement placeKeystoneAt(int kx, int ky, int kz, Facing facing) {
        int[] offset = facing.toWorld(keystone.x, keystone.y, keystone.z);
        return new Placement(kx - offset[0], ky - offset[1], kz - offset[2], facing);
    }

    /** This frame laid out with its local origin {@code (0,0,0)} at the given world position. */
    public Placement placeOriginAt(int ox, int oy, int oz, Facing facing) {
        return new Placement(ox, oy, oz, facing);
    }

    /**
     * This frame laid out with its footprint centred on {@code (x, z)} and its bottom layer at
     * {@code y} &mdash; how worldgen stands a frame on a chosen spot.
     */
    public Placement placeCentredAt(int x, int y, int z, Facing facing) {
        int maxX = 0;
        int maxZ = 0;
        for (Cell cell : cells) {
            maxX = Math.max(maxX, cell.x);
            maxZ = Math.max(maxZ, cell.z);
        }
        int[] centre = facing.toWorld(maxX / 2, 0, maxZ / 2);
        return new Placement(x - centre[0], y, z - centre[2], facing);
    }

    /**
     * This frame laid out so that its {@link Placement#arrivalSpot} lands exactly on
     * {@code (x, y, z)} &mdash; how a frame is put in front of someone standing there.
     */
    public Placement placeArrivalAt(int x, int y, int z, Facing facing) {
        int[] arrival = new Placement(0, 0, 0, facing).arrivalSpot();
        return new Placement(x - arrival[0], y - arrival[1], z - arrival[2], facing);
    }

    /**
     * The direction a frame's front faces, i.e. where someone looking at it from the front stands.
     * Local {@code +x} runs to that viewer's right, {@code +z} (back) runs away from them.
     */
    public enum Facing {
        SOUTH(1, 0, 0, -1),
        WEST(0, 1, 1, 0),
        NORTH(-1, 0, 0, 1),
        EAST(0, -1, -1, 0);

        private final int rightX;
        private final int rightZ;
        private final int backX;
        private final int backZ;

        Facing(int rightX, int rightZ, int backX, int backZ) {
            this.rightX = rightX;
            this.rightZ = rightZ;
            this.backX = backX;
            this.backZ = backZ;
        }

        int[] toWorld(int x, int y, int z) {
            return new int[] {x * rightX + z * backX, y, x * rightZ + z * backZ};
        }

        /** Unit vector pointing out of the frame's front, toward the viewer. */
        public int frontX() {
            return -backX;
        }

        public int frontZ() {
            return -backZ;
        }

        /** Whether the field's plane runs along the world X axis (else along Z). */
        public boolean fieldAlongX() {
            return rightX != 0;
        }
    }

    /** A frame laid out in the world: local cells mapped through a {@link Facing} from an origin. */
    public final class Placement {

        private final int originX;
        private final int originY;
        private final int originZ;
        private final Facing facing;

        Placement(int originX, int originY, int originZ, Facing facing) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.facing = facing;
        }

        public Facing facing() {
            return facing;
        }

        /** The frame this is a placement of. */
        public GatewayFrame frame() {
            return GatewayFrame.this;
        }

        /** World position of a local cell, as {@code {x, y, z}}. */
        public int[] world(int x, int y, int z) {
            int[] offset = facing.toWorld(x, y, z);
            return new int[] {originX + offset[0], originY + offset[1], originZ + offset[2]};
        }

        boolean matches(CellTest test) {
            for (Cell cell : cells) {
                int[] at = world(cell.x, cell.y, cell.z);
                if (!test.matches(cell.symbol, at[0], at[1], at[2])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Whether a block of the frame itself stands at this world position: any cell but the
         * opening. What makes a pre-generated gate unbreakable ({@code PregeneratedGates}).
         */
        public boolean isFrameBlock(int x, int y, int z) {
            for (Cell cell : cells) {
                if (cell.symbol == fieldSymbol) {
                    continue;
                }
                int[] at = world(cell.x, cell.y, cell.z);
                if (at[0] == x && at[1] == y && at[2] == z) {
                    return true;
                }
            }
            return false;
        }

        /** World positions of every field cell. */
        public List<int[]> fieldPositions() {
            List<int[]> positions = new ArrayList<>();
            for (Cell cell : cells) {
                if (cell.symbol == fieldSymbol) {
                    positions.add(world(cell.x, cell.y, cell.z));
                }
            }
            return positions;
        }

        /**
         * Where someone arriving should stand: on the field's bottom row, horizontally centred,
         * one block out in front of it.
         */
        public int[] arrivalSpot() {
            int[] bottomCentre = world((fieldMinX + fieldMaxX) / 2, fieldMinY, fieldZ);
            return new int[] {bottomCentre[0] + facing.frontX(), bottomCentre[1], bottomCentre[2] + facing.frontZ()};
        }

        public int[] keystone() {
            return world(GatewayFrame.this.keystone.x, GatewayFrame.this.keystone.y, GatewayFrame.this.keystone.z);
        }

        /** World-space bounds of every cell, as {@code {minX, minY, minZ, maxX, maxY, maxZ}}. */
        public int[] bounds() {
            int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (Cell cell : cells) {
                int[] at = world(cell.x, cell.y, cell.z);
                for (int i = 0; i < 3; i++) {
                    bounds[i] = Math.min(bounds[i], at[i]);
                    bounds[i + 3] = Math.max(bounds[i + 3], at[i]);
                }
            }
            return bounds;
        }
    }
}
