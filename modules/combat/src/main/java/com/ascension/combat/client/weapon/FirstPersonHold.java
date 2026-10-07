package com.ascension.combat.client.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * How a weapon is held in first person, with the player's own arms on it: the
 * {@code "first_person"} section of its {@code weapon_models} file, written by
 * {@code tools/generate-combat-models.py} and drawn by {@link WeaponFirstPerson}.
 *
 * <p>A pose places the weapon by its right-hand grip point: {@code pos} is where that point sits
 * in front of the camera (blocks: x right, y up, -z ahead, as vanilla's held items), and
 * {@code rotation} turns the weapon about it (degrees, Z . Y . X, X first, as the model's parts).
 * The right hand is drawn on {@code right_hand} and, when the pose says {@code two_hands}, the
 * left on {@code left_hand} (model pixels); each arm reaches back toward its shoulder, off the
 * bottom of the screen. Attacks are keyframes over the swing like {@link HoldAnimation}'s, and the
 * same attack is picked as in third person. Written right-handed; left-handed players see it
 * mirrored.
 */
final class FirstPersonHold {
    record Pose(float[] pos, float[] rotation, boolean twoHands) {
    }

    private record Key(float t, Pose pose) {
    }

    final float scale;
    final float[] rightHand;
    final float[] leftHand;
    final float[] rightShoulder;
    final float[] leftShoulder;
    final Pose idle;
    private final List<List<Key>> attacks;

    private FirstPersonHold(float scale, float[] rightHand, float[] leftHand, float[] rightShoulder,
                            float[] leftShoulder, Pose idle, List<List<Key>> attacks) {
        this.scale = scale;
        this.rightHand = rightHand;
        this.leftHand = leftHand;
        this.rightShoulder = rightShoulder;
        this.leftShoulder = leftShoulder;
        this.idle = idle;
        this.attacks = attacks;
    }

    int attackCount() {
        return attacks.size();
    }

    /** Attack {@code index} at swing progress {@code t}, eased between its keyframes. */
    Pose attack(int index, float t) {
        List<Key> keys = attacks.get(index);
        if (t <= keys.get(0).t) {
            return keys.get(0).pose;
        }
        for (int i = 1; i < keys.size(); i++) {
            Key a = keys.get(i - 1), b = keys.get(i);
            if (t <= b.t) {
                float f = (t - a.t) / Math.max(1e-6f, b.t - a.t);
                f = f * f * (3 - 2 * f);
                return new Pose(lerp(a.pose.pos, b.pose.pos, f), lerp(a.pose.rotation, b.pose.rotation, f),
                        f < 0.5f ? a.pose.twoHands : b.pose.twoHands);
            }
        }
        return keys.get(keys.size() - 1).pose;
    }

    private static float[] lerp(float[] a, float[] b, float f) {
        return new float[]{Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])};
    }

    static FirstPersonHold parse(JsonObject json) {
        List<List<Key>> attacks = new ArrayList<>();
        for (JsonElement attack : GsonHelper.getAsJsonArray(json, "attacks")) {
            List<Key> keys = new ArrayList<>();
            for (JsonElement key : attack.getAsJsonArray()) {
                JsonObject k = key.getAsJsonObject();
                keys.add(new Key(GsonHelper.getAsFloat(k, "t"), pose(k)));
            }
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("a first-person attack with no keyframes");
            }
            attacks.add(List.copyOf(keys));
        }
        if (attacks.isEmpty()) {
            throw new IllegalArgumentException("a first-person hold with no attacks");
        }
        return new FirstPersonHold(GsonHelper.getAsFloat(json, "scale"), vec(json, "right_hand"),
                vec(json, "left_hand"), vec(json, "right_shoulder"), vec(json, "left_shoulder"),
                pose(GsonHelper.getAsJsonObject(json, "idle")), List.copyOf(attacks));
    }

    private static Pose pose(JsonObject json) {
        return new Pose(vec(json, "pos"), vec(json, "rotation"), GsonHelper.getAsBoolean(json, "two_hands", false));
    }

    private static float[] vec(JsonObject json, String key) {
        JsonArray a = GsonHelper.getAsJsonArray(json, key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
