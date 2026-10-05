package com.ascension.worlds.internal.gateway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Vec3i;

/**
 * Decoration around a pre-generated gateway frame &mdash; the Moon's ruined plaza, pillars and
 * rubble. Same {@code key}/{@code layers} grid as a {@link GatewayFrame} (no keystone, no field),
 * plus {@code frame_offset}: where the frame's own cell {@code (0,0,0)} sits inside this grid.
 * Placed by worldgen only, never validated. Format: {@code docs/technical/gateway-frame-format.md}.
 *
 * <p>Lives in its own registry ({@code ascension_worlds/gateway_site}), not beside the frames: a
 * datapack registry parses every file in its folder with one codec, and a site is not a frame.
 */
public final class GatewaySite {

    static final int MAX_WIDTH = GatewayFrame.MAX_WIDTH * 2;
    static final int MAX_HEIGHT = GatewayFrame.MAX_HEIGHT * 2;
    static final int MAX_DEPTH = GatewayFrame.MAX_DEPTH * 2;

    private record Raw(Map<String, GatewayFrame.Key> key, List<List<String>> layers, Vec3i frameOffset) {
    }

    public static final Codec<GatewaySite> CODEC = RecordCodecBuilder.<Raw>create(instance -> instance
                    .group(
                            Codec.unboundedMap(Codec.STRING, GatewayFrame.Key.CODEC).fieldOf("key").forGetter(Raw::key),
                            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(Raw::layers),
                            Vec3i.CODEC.fieldOf("frame_offset").forGetter(Raw::frameOffset))
                    .apply(instance, Raw::new))
            .flatXmap(GatewaySite::build, site -> DataResult.success(site.raw));

    private final Raw raw;
    private final Map<Character, GatewayFrame.Key> key;
    private final List<GatewayFrame.Cell> cells;

    private GatewaySite(Raw raw, Map<Character, GatewayFrame.Key> key, List<GatewayFrame.Cell> cells) {
        this.raw = raw;
        this.key = key;
        this.cells = cells;
    }

    static DataResult<GatewaySite> build(Raw raw) {
        Map<Character, GatewayFrame.Key> key = new HashMap<>();
        for (Map.Entry<String, GatewayFrame.Key> entry : raw.key.entrySet()) {
            if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
                return DataResult.error(() -> "site key \"" + entry.getKey() + "\" must be one non-space character");
            }
            key.put(entry.getKey().charAt(0), entry.getValue());
        }
        if (raw.layers.isEmpty() || raw.layers.size() > MAX_HEIGHT) {
            return DataResult.error(() -> "site needs 1 to " + MAX_HEIGHT + " layers, got " + raw.layers.size());
        }
        List<GatewayFrame.Cell> cells = new ArrayList<>();
        for (int y = 0; y < raw.layers.size(); y++) {
            List<String> rows = raw.layers.get(y);
            if (rows.size() > MAX_DEPTH) {
                int layer = y;
                return DataResult.error(() -> "site layer " + layer + " is deeper than " + MAX_DEPTH);
            }
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                if (row.length() > MAX_WIDTH) {
                    int layer = y;
                    int depth = z;
                    return DataResult.error(() -> "site layer " + layer + " row " + depth + " is wider than " + MAX_WIDTH);
                }
                for (int x = 0; x < row.length(); x++) {
                    char symbol = row.charAt(x);
                    if (symbol == ' ') {
                        continue;
                    }
                    if (!key.containsKey(symbol)) {
                        String where = "layer " + y + " row " + z + " column " + x;
                        return DataResult.error(() -> "unknown site character '" + symbol + "' at " + where);
                    }
                    cells.add(new GatewayFrame.Cell(x, y, z, symbol));
                }
            }
        }
        return DataResult.success(new GatewaySite(raw, Map.copyOf(key), List.copyOf(cells)));
    }

    public Map<Character, GatewayFrame.Key> key() {
        return key;
    }

    /** Every non-space cell, in this site's own grid. */
    public List<GatewayFrame.Cell> cells() {
        return cells;
    }

    /** Where the frame's cell (0,0,0) sits in this site's grid. */
    public Vec3i frameOffset() {
        return raw.frameOffset;
    }

    /**
     * World position of a site cell, given where the frame was laid out: site cells are expressed
     * in the frame's own local space (shifted by {@link #frameOffset}), so they turn with it.
     */
    public int[] world(GatewayFrame.Placement frame, GatewayFrame.Cell cell) {
        Vec3i offset = raw.frameOffset;
        return frame.world(cell.x() - offset.getX(), cell.y() - offset.getY(), cell.z() - offset.getZ());
    }
}
