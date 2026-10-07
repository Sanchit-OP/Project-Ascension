package com.ascension.combat.client.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One held weapon's geometry, read from {@code assets/<ns>/weapon_models/<item>.json}, which
 * {@code tools/generate-combat-models.py} writes. Parts nest, each turned about its own pivot by
 * any angle (Z . Y . X, X applied first, as vanilla's {@code ModelPart}); a part holds axis-aligned
 * cubes whose six faces each name a texel rectangle of the texture, corner for corner as vanilla
 * block-model faces. Coordinates are model pixels in the item's own frame (0..16 is one block),
 * the same frame a JSON item model's {@code display} transforms place.
 *
 * <p>The tree is flattened once, here, at load: rendering walks a plain float array. No part moves
 * yet, so there is nothing to keep but the quads. How it is held and swung is not here: held, it
 * sits in the fist like any sword (the item model's display transforms), and an animation mod
 * such as Better Combat animates the holder (ascension-compat-bettercombat).
 */
public final class WeaponModel {
    /** Per quad: four vertices of (x, y, z, u, v), then the face normal (x, y, z). */
    static final int QUAD_FLOATS = 4 * 5 + 3;

    private static final Map<String, int[][]> FACE_CORNERS = Map.of(
            "down", new int[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
            "up", new int[][]{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            "north", new int[][]{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
            "south", new int[][]{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
            "west", new int[][]{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
            "east", new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}});

    private final ResourceLocation texture;
    private final ResourceLocation glowTexture;
    private final float[] quads;

    private WeaponModel(ResourceLocation texture, ResourceLocation glowTexture, float[] quads) {
        this.texture = texture;
        this.glowTexture = glowTexture;
        this.quads = quads;
    }

    public ResourceLocation texture() {
        return texture;
    }

    /** The emissive layer drawn over the model, or null if nothing on it glows. */
    public ResourceLocation glowTexture() {
        return glowTexture;
    }

    float[] quads() {
        return quads;
    }

    static WeaponModel parse(JsonObject json) {
        ResourceLocation texture = ResourceLocation.parse(GsonHelper.getAsString(json, "texture"));
        ResourceLocation glow = json.has("glow_texture")
                ? ResourceLocation.parse(GsonHelper.getAsString(json, "glow_texture")) : null;
        JsonArray size = GsonHelper.getAsJsonArray(json, "texture_size");
        float texW = size.get(0).getAsFloat();
        float texH = size.get(1).getAsFloat();
        List<float[]> out = new ArrayList<>();
        for (JsonElement part : GsonHelper.getAsJsonArray(json, "parts")) {
            addPart(part.getAsJsonObject(), new Matrix4f(), texW, texH, out);
        }
        float[] flat = new float[out.size() * QUAD_FLOATS];
        for (int i = 0; i < out.size(); i++) {
            System.arraycopy(out.get(i), 0, flat, i * QUAD_FLOATS, QUAD_FLOATS);
        }
        return new WeaponModel(texture, glow, flat);
    }

    private static void addPart(JsonObject part, Matrix4f parent, float texW, float texH, List<float[]> out) {
        Vector3f pivot = vec(part, "pivot");
        Vector3f rot = vec(part, "rotation");
        Matrix4f m = new Matrix4f(parent)
                .translate(pivot)
                .rotateZ((float) Math.toRadians(rot.z))
                .rotateY((float) Math.toRadians(rot.y))
                .rotateX((float) Math.toRadians(rot.x))
                .translate(-pivot.x, -pivot.y, -pivot.z);
        if (part.has("cubes")) {
            for (JsonElement cube : part.getAsJsonArray("cubes")) {
                addCube(cube.getAsJsonObject(), m, texW, texH, out);
            }
        }
        if (part.has("children")) {
            for (JsonElement child : part.getAsJsonArray("children")) {
                addPart(child.getAsJsonObject(), m, texW, texH, out);
            }
        }
    }

    private static void addCube(JsonObject cube, Matrix4f m, float texW, float texH, List<float[]> out) {
        Vector3f from = vec(cube, "from");
        Vector3f to = vec(cube, "to");
        JsonObject faces = GsonHelper.getAsJsonObject(cube, "faces");
        for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
            int[][] corners = FACE_CORNERS.get(face.getKey());
            if (corners == null) {
                throw new IllegalArgumentException("unknown face " + face.getKey());
            }
            JsonArray uv = face.getValue().getAsJsonArray();
            float u0 = uv.get(0).getAsFloat() / texW;
            float v0 = uv.get(1).getAsFloat() / texH;
            float u1 = uv.get(2).getAsFloat() / texW;
            float v1 = uv.get(3).getAsFloat() / texH;
            float[] q = new float[QUAD_FLOATS];
            Vector3f[] p = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                p[i] = m.transformPosition(new Vector3f(
                        corners[i][0] == 1 ? to.x : from.x,
                        corners[i][1] == 1 ? to.y : from.y,
                        corners[i][2] == 1 ? to.z : from.z));
                q[i * 5] = p[i].x;
                q[i * 5 + 1] = p[i].y;
                q[i * 5 + 2] = p[i].z;
                q[i * 5 + 3] = (i == 0 || i == 1) ? u0 : u1;
                q[i * 5 + 4] = (i == 0 || i == 3) ? v0 : v1;
            }
            Vector3f n = new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0]));
            if (n.lengthSquared() < 1e-12f) {
                n = new Vector3f(p[2]).sub(p[0]).cross(new Vector3f(p[3]).sub(p[0]));
            }
            if (n.lengthSquared() < 1e-12f) {
                continue; // a face of zero area draws nothing
            }
            n.normalize();
            q[20] = n.x;
            q[21] = n.y;
            q[22] = n.z;
            out.add(q);
        }
    }

    private static Vector3f vec(JsonObject json, String key) {
        if (!json.has(key)) {
            return new Vector3f();
        }
        JsonArray a = json.getAsJsonArray(key);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }
}
