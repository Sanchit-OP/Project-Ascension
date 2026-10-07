package com.ascension.combat.client.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * How a weapon is held and swung by a humanoid in third person: the {@code "hold"} section of its
 * {@code weapon_models} file, written by {@code tools/generate-combat-models.py}.
 *
 * <p>A pose sets the right arm, optionally the left (then the weapon is held in both hands), the
 * body's twist, and an extra turn of the weapon about its grip; angles in degrees, as the model's
 * part rotations. (Only the twist: vanilla sets the body's lean for crouching after this runs.) {@code idle} is held standing and walking, with {@code walk_swing} of vanilla's arm
 * swing left on the right arm. Each attack is a list of keyframes over the swing (t from 0 to 1),
 * eased between; which attack plays is picked per swing. Written for a right-handed holder; the
 * left hand mirrors it.
 */
final class HoldAnimation {
    /** One pose: rightArm, leftArm (NaN x: not held) and item as (x, y, z), bodyTwist; degrees. */
    record Pose(float[] rightArm, float[] leftArm, float bodyTwist, float[] item) {
        boolean twoHanded() {
            return !Float.isNaN(leftArm[0]);
        }
    }

    private record Key(float t, Pose pose) {
    }

    final float[] grip;
    final Pose idle;
    final float walkSwing;
    private final List<List<Key>> attacks;

    private HoldAnimation(float[] grip, Pose idle, float walkSwing, List<List<Key>> attacks) {
        this.grip = grip;
        this.idle = idle;
        this.walkSwing = walkSwing;
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
                return lerp(a.pose, b.pose, f * f * (3 - 2 * f));
            }
        }
        return keys.get(keys.size() - 1).pose;
    }

    private static Pose lerp(Pose a, Pose b, float f) {
        float[] left;
        if (a.twoHanded() && b.twoHanded()) {
            left = lerp(a.leftArm, b.leftArm, f);
        } else {
            left = f < 0.5f ? a.leftArm : b.leftArm;
        }
        return new Pose(lerp(a.rightArm, b.rightArm, f), left, Mth.lerp(f, a.bodyTwist, b.bodyTwist),
                lerp(a.item, b.item, f));
    }

    private static float[] lerp(float[] a, float[] b, float f) {
        return new float[]{Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])};
    }

    static HoldAnimation parse(JsonObject json) {
        float[] grip = vec(json, "grip", 0);
        Pose idle = pose(GsonHelper.getAsJsonObject(json, "idle"));
        float walkSwing = GsonHelper.getAsFloat(json, "walk_swing", 1f);
        List<List<Key>> attacks = new ArrayList<>();
        for (JsonElement attack : GsonHelper.getAsJsonArray(json, "attacks")) {
            List<Key> keys = new ArrayList<>();
            for (JsonElement key : attack.getAsJsonArray()) {
                JsonObject k = key.getAsJsonObject();
                keys.add(new Key(GsonHelper.getAsFloat(k, "t"), pose(k)));
            }
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("an attack with no keyframes");
            }
            attacks.add(List.copyOf(keys));
        }
        if (attacks.isEmpty()) {
            throw new IllegalArgumentException("a hold with no attacks");
        }
        return new HoldAnimation(grip, idle, walkSwing, List.copyOf(attacks));
    }

    private static Pose pose(JsonObject json) {
        return new Pose(vec(json, "right_arm", 0), vec(json, "left_arm", Float.NaN),
                GsonHelper.getAsFloat(json, "body_twist", 0), vec(json, "item", 0));
    }

    private static float[] vec(JsonObject json, String key, float missing) {
        if (!json.has(key)) {
            return new float[]{missing, missing, missing};
        }
        JsonArray a = json.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
