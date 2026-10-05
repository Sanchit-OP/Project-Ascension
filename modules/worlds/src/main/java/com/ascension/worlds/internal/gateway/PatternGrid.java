package com.ascension.worlds.internal.gateway;

import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The character-grid half of the gateway formats, shared by {@link GatewayFrame} and
 * {@link GatewaySite}: a {@code key} of single characters, and {@code layers} (bottom to top) of
 * rows (front to back) of cells (left to right), where a space means "nothing here".
 * Each format adds its own rules on top; this only reads the grid and enforces size limits.
 */
final class PatternGrid {

    private PatternGrid() {
    }

    /**
     * @param label prefixed to every error ({@code ""} for a frame, {@code "site "} for a site),
     *              so a load error says which file kind it came from
     */
    static DataResult<Map<Character, GatewayFrame.Key>> parseKey(Map<String, GatewayFrame.Key> raw, String label) {
        Map<Character, GatewayFrame.Key> key = new HashMap<>();
        for (Map.Entry<String, GatewayFrame.Key> entry : raw.entrySet()) {
            if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
                return DataResult.error(() -> label + "key \"" + entry.getKey() + "\" must be one non-space character");
            }
            key.put(entry.getKey().charAt(0), entry.getValue());
        }
        return DataResult.success(Map.copyOf(key));
    }

    /** Every non-space cell, after checking the grid fits the limits and uses only allowed characters. */
    static DataResult<List<GatewayFrame.Cell>> parseLayers(List<List<String>> layers, int maxWidth, int maxHeight,
                                                         int maxDepth, Predicate<Character> allowed, String label) {
        if (layers.isEmpty() || layers.size() > maxHeight) {
            return DataResult.error(() -> label + "needs 1 to " + maxHeight + " layers, got " + layers.size());
        }
        List<GatewayFrame.Cell> cells = new ArrayList<>();
        for (int y = 0; y < layers.size(); y++) {
            List<String> rows = layers.get(y);
            int layer = y;
            if (rows.size() > maxDepth) {
                return DataResult.error(() -> label + "layer " + layer + " is deeper than " + maxDepth);
            }
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                int depth = z;
                if (row.length() > maxWidth) {
                    return DataResult.error(() -> label + "layer " + layer + " row " + depth + " is wider than " + maxWidth);
                }
                for (int x = 0; x < row.length(); x++) {
                    char symbol = row.charAt(x);
                    if (symbol == ' ') {
                        continue;
                    }
                    if (!allowed.test(symbol)) {
                        String where = "layer " + y + " row " + z + " column " + x;
                        return DataResult.error(() -> "unknown " + label + "character '" + symbol + "' at " + where);
                    }
                    cells.add(new GatewayFrame.Cell(x, y, z, symbol));
                }
            }
        }
        return DataResult.success(List.copyOf(cells));
    }
}
