package com.example.katanas.client;

import com.example.katanas.ElementalKatanas;
import com.example.katanas.item.KatanaItem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Client-only. Replaces vanilla's first-person swing for katanas with:
 *  - an idle grip per katana (+ subtle breathing sway)
 *  - a draw animation while the katana is being raised (equip)
 *  - a 4-technique attack combo on its own timeline (not tied to vanilla's 6-tick swing)
 *  - a longer, per-katana ability animation when right-click fires
 *
 * Called from HeldItemRendererMixin once per frame for the katana in hand.
 */
public final class KatanaAnimator {
    private KatanaAnimator() {}

    private enum Mode { NONE, ATTACK, ABILITY }

    private static Mode mode = Mode.NONE;
    private static float lastSwingProgress = 0f;
    private static float swingStart = -1000f;
    private static float lastAttackTime = -1000f;
    private static int technique = 0;
    private static boolean logged = false;

    public static void transform(MatrixStack m, KatanaItem katana, boolean rightArm, boolean mainHand,
                                 float swingProgress, float equipProgress, float now) {
        if (!logged) {
            logged = true;
            ElementalKatanas.LOGGER.info("Katana first-person animator is active.");
        }

        float s = rightArm ? 1f : -1f;
        String id = Registries.ITEM.getId(katana).getPath();
        KatanaPoses.Style style = KatanaPoses.styleFor(id);

        if (mainHand) {
            trackSwing(swingProgress, now);
        }

        Pose pose = style.idle();
        if (mainHand && mode != Mode.NONE) {
            float duration = mode == Mode.ABILITY ? style.abilityTicks() : KatanaPoses.ATTACK_TICKS;
            float elapsed = now - swingStart;
            if (elapsed >= 0f && elapsed < duration) {
                KatanaPoses.Key[] keys = mode == Mode.ABILITY ? style.ability() : KatanaPoses.ATTACK[technique];
                pose = pose.add(KatanaPoses.sample(keys, elapsed / duration));
            } else {
                mode = Mode.NONE;
            }
        }

        // Breathing sway so the blade never looks frozen.
        float sway = MathHelper.sin(now * 0.08f);
        pose = pose.add(new Pose(0f, sway * 0.006f, 0f, sway * 0.5f, 0f, sway * 0.8f));

        // Same base placement as vanilla's equip offset.
        m.translate(s * 0.56f, -0.52f - 0.6f * equipProgress, -0.72f);

        // Draw: the blade swings up from low and rolled while it is being raised.
        if (equipProgress > 0f) {
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s * -55f * equipProgress));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35f * equipProgress));
        }

        m.translate(s * pose.tx(), pose.ty(), pose.tz());
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * pose.ry()));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.rx()));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s * pose.rz()));
    }

    /** Detects the start of a swing (progress leaves 0, or restarts mid-swing) and picks the animation. */
    private static void trackSwing(float swingProgress, float now) {
        boolean swinging = swingProgress > 0f;
        boolean started = swinging && (lastSwingProgress == 0f || swingProgress < lastSwingProgress - 0.2f);
        lastSwingProgress = swingProgress;
        if (!started) return;

        swingStart = now;
        if (Math.abs(now - KatanaItem.clientAbilityStartAge) <= 3f) {
            mode = Mode.ABILITY;
        } else {
            technique = (now - lastAttackTime > KatanaPoses.COMBO_RESET_TICKS) ? 0 : (technique + 1) % 4;
            lastAttackTime = now;
            mode = Mode.ATTACK;
        }
    }
}
