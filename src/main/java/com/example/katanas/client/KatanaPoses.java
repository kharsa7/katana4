package com.example.katanas.client;

import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * ALL animation numbers live here. Poses are ADDED on top of the katana's idle grip.
 * Keyframe time t runs 0..1 over the whole animation; each key's Ease shapes the segment that ENDS at it
 * (OUT = fast start, slow end: used for the strike so it snaps like a real cut).
 *
 * Attack combo (real katana techniques), cycles on every swing, resets after ~1.2s idle:
 *   0 kesa-giri   diagonal cut, upper right -> lower left
 *   1 yoko-giri   horizontal cut, right -> left
 *   2 gyaku-kesa  rising diagonal cut, lower left -> upper right
 *   3 tsuki       straight thrust
 *
 * If a direction looks mirrored in game, flip the sign of that axis in the keys below.
 */
public final class KatanaPoses {
    private KatanaPoses() {}

    public enum Ease { LINEAR, SMOOTH, OUT, IN }

    public record Key(float t, Ease ease, Pose pose) {}

    /** idle = grip (how the katana is held), ability = right-click animation. */
    public record Style(Pose idle, int abilityTicks, Key[] ability) {}

    public static final int ATTACK_TICKS = 8;
    public static final float COMBO_RESET_TICKS = 24f;

    private static Pose p(float tx, float ty, float tz, float rx, float ry, float rz) {
        return new Pose(tx, ty, tz, rx, ry, rz);
    }

    private static Key k(float t, Ease e, Pose pose) {
        return new Key(t, e, pose);
    }

    public static final Key[][] ATTACK = {
            // 0: kesa-giri
            {
                    k(0.00f, Ease.LINEAR, Pose.ZERO),
                    k(0.28f, Ease.SMOOTH, p(0.10f, 0.22f, 0.10f, -35f, 10f, 30f)),
                    k(0.50f, Ease.OUT, p(-0.30f, -0.26f, -0.28f, 30f, -25f, -55f)),
                    k(0.68f, Ease.SMOOTH, p(-0.32f, -0.28f, -0.30f, 34f, -28f, -60f)),
                    k(1.00f, Ease.SMOOTH, Pose.ZERO)
            },
            // 1: yoko-giri
            {
                    k(0.00f, Ease.LINEAR, Pose.ZERO),
                    k(0.28f, Ease.SMOOTH, p(0.32f, 0.02f, 0.12f, 0f, -45f, 12f)),
                    k(0.50f, Ease.OUT, p(-0.52f, -0.02f, -0.25f, 0f, 55f, -10f)),
                    k(0.68f, Ease.SMOOTH, p(-0.55f, -0.02f, -0.27f, 0f, 60f, -12f)),
                    k(1.00f, Ease.SMOOTH, Pose.ZERO)
            },
            // 2: gyaku-kesa (rising)
            {
                    k(0.00f, Ease.LINEAR, Pose.ZERO),
                    k(0.28f, Ease.SMOOTH, p(-0.30f, -0.30f, 0.10f, 20f, 35f, -40f)),
                    k(0.50f, Ease.OUT, p(0.30f, 0.34f, -0.25f, -30f, -30f, 50f)),
                    k(0.68f, Ease.SMOOTH, p(0.32f, 0.36f, -0.27f, -34f, -33f, 54f)),
                    k(1.00f, Ease.SMOOTH, Pose.ZERO)
            },
            // 3: tsuki (thrust)
            {
                    k(0.00f, Ease.LINEAR, Pose.ZERO),
                    k(0.30f, Ease.SMOOTH, p(0.10f, -0.05f, 0.32f, 10f, 20f, -20f)),
                    k(0.50f, Ease.OUT, p(0.00f, 0.02f, -0.65f, -12f, -5f, 5f)),
                    k(0.70f, Ease.SMOOTH, p(0.00f, 0.02f, -0.68f, -12f, -5f, 5f)),
                    k(1.00f, Ease.SMOOTH, Pose.ZERO)
            }
    };

    private static final Style DEFAULT = new Style(Pose.ZERO, 16, new Key[]{
            k(0.00f, Ease.LINEAR, Pose.ZERO),
            k(0.40f, Ease.SMOOTH, p(0f, 0.30f, 0.10f, -50f, 0f, 0f)),
            k(0.55f, Ease.OUT, p(0f, -0.15f, -0.40f, 40f, 0f, 0f)),
            k(1.00f, Ease.SMOOTH, Pose.ZERO)
    });

    private static final Map<String, Style> STYLES = new HashMap<>();

    static {
        // FROST: low guard stance (chudan), ability = overhead raise and slam forward.
        STYLES.put("frost_katana", new Style(p(0.00f, -0.04f, 0.00f, 8f, -6f, -10f), 18, new Key[]{
                k(0.00f, Ease.LINEAR, Pose.ZERO),
                k(0.30f, Ease.SMOOTH, p(0.05f, 0.35f, 0.15f, -55f, 0f, 15f)),
                k(0.45f, Ease.OUT, p(-0.05f, -0.15f, -0.45f, 40f, 0f, -5f)),
                k(0.75f, Ease.SMOOTH, p(-0.05f, -0.16f, -0.46f, 42f, 0f, -5f)),
                k(1.00f, Ease.SMOOTH, Pose.ZERO)
        }));

        // FLAME: raised stance (jodan), ability = iai draw: pull to the hip, then wide horizontal cut.
        STYLES.put("flame_katana", new Style(p(0.04f, 0.06f, 0.02f, -10f, -4f, 14f), 16, new Key[]{
                k(0.00f, Ease.LINEAR, Pose.ZERO),
                k(0.30f, Ease.SMOOTH, p(0.35f, -0.30f, 0.25f, 15f, -50f, 25f)),
                k(0.46f, Ease.OUT, p(-0.55f, 0.00f, -0.35f, -5f, 75f, -15f)),
                k(0.62f, Ease.SMOOTH, p(-0.60f, 0.02f, -0.36f, -5f, 80f, -18f)),
                k(1.00f, Ease.SMOOTH, Pose.ZERO)
        }));

        // LIGHTNING: low reverse grip, blade nearly horizontal. Ability = blade straight to the sky, stab down.
        STYLES.put("lightning_katana", new Style(p(-0.02f, -0.02f, 0.04f, 4f, 10f, -28f), 20, new Key[]{
                k(0.00f, Ease.LINEAR, Pose.ZERO),
                k(0.35f, Ease.SMOOTH, p(0.00f, 0.45f, 0.05f, -75f, 0f, 0f)),
                k(0.50f, Ease.OUT, p(0.00f, -0.20f, -0.40f, 55f, 0f, 0f)),
                k(0.80f, Ease.SMOOTH, p(0.00f, -0.21f, -0.41f, 56f, 0f, 0f)),
                k(1.00f, Ease.SMOOTH, Pose.ZERO)
        }));

        // GRAVITY: blade held across the body (like the purple katana in the reference). Ability = vortex spin, then slam.
        STYLES.put("gravity_katana", new Style(p(-0.04f, 0.02f, 0.00f, 0f, 14f, -30f), 22, new Key[]{
                k(0.00f, Ease.LINEAR, Pose.ZERO),
                k(0.15f, Ease.SMOOTH, p(0.00f, 0.10f, 0.10f, -10f, 0f, -60f)),
                k(0.35f, Ease.LINEAR, p(0.00f, 0.15f, 0.00f, 0f, 0f, -200f)),
                k(0.55f, Ease.LINEAR, p(0.00f, 0.15f, 0.00f, 0f, 0f, -330f)),
                k(0.75f, Ease.OUT, p(0.00f, -0.30f, -0.40f, 45f, 0f, -360f)),
                k(1.00f, Ease.SMOOTH, p(0f, 0f, 0f, 0f, 0f, -360f)) // -360 deg == 0: looks like a clean return
        }));

        // SPACE: upright centre-line stance (seigan). Ability = lunging thrust into a sweeping slash.
        STYLES.put("space_katana", new Style(p(0.00f, 0.05f, -0.02f, -6f, 0f, 8f), 16, new Key[]{
                k(0.00f, Ease.LINEAR, Pose.ZERO),
                k(0.20f, Ease.SMOOTH, p(0.10f, 0.00f, 0.35f, 5f, 10f, -15f)),
                k(0.35f, Ease.OUT, p(0.00f, 0.00f, -0.70f, -10f, 0f, 5f)),
                k(0.55f, Ease.SMOOTH, p(0.40f, 0.00f, -0.30f, 0f, -60f, 10f)),
                k(0.70f, Ease.OUT, p(-0.55f, 0.00f, -0.30f, 0f, 65f, -10f)),
                k(1.00f, Ease.SMOOTH, Pose.ZERO)
        }));
    }

    public static Style styleFor(String itemPath) {
        return STYLES.getOrDefault(itemPath, DEFAULT);
    }

    public static Pose sample(Key[] keys, float t) {
        Key prev = keys[0];
        for (int i = 1; i < keys.length; i++) {
            Key key = keys[i];
            if (t <= key.t()) {
                float local = (t - prev.t()) / Math.max(1.0E-4f, key.t() - prev.t());
                return Pose.lerp(prev.pose(), key.pose(), ease(key.ease(), MathHelper.clamp(local, 0f, 1f)));
            }
            prev = key;
        }
        return prev.pose();
    }

    private static float ease(Ease e, float t) {
        return switch (e) {
            case LINEAR -> t;
            case SMOOTH -> t * t * (3f - 2f * t);
            case OUT -> 1f - (1f - t) * (1f - t) * (1f - t);
            case IN -> t * t * t;
        };
    }
}
