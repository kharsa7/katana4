package com.example.katanas.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;

/**
 * Particle + sound helpers. Everything here is server-side: ServerWorld#spawnParticles
 * broadcasts to every nearby player, so other players see the effects too.
 *
 * To use CUSTOM sounds later: register SoundEvents in a ModSounds class, add
 * assets/elementalkatanas/sounds.json + .ogg files, and swap them in via {@link #sound}.
 */
public final class KatanaFX {
    private KatanaFX() {}

    /** Approximate world position of a point along the held blade. */
    public static Vec3d bladePoint(LivingEntity e, double along) {
        Vec3d look = e.getRotationVec(1.0f);
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
        double side = e.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;
        Vec3d hand = e.getEyePos().add(right.multiply(0.35 * side)).add(0, -0.55, 0);
        return hand.add(look.multiply(along));
    }

    /** Scatter a few particles along the blade (used for ambient effects). */
    public static void sprinkle(ServerWorld w, ParticleEffect p, LivingEntity holder, int count, double speed) {
        for (int i = 0; i < count; i++) {
            Vec3d pt = bladePoint(holder, 0.5 + holder.getRandom().nextDouble() * 1.1);
            w.spawnParticles(p, pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, speed);
        }
    }

    public static void burst(ServerWorld w, ParticleEffect p, Vec3d pos, int count, double spread, double speed) {
        w.spawnParticles(p, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public static void line(ServerWorld w, ParticleEffect p, Vec3d from, Vec3d to,
                            double step, int perPoint, double spread) {
        Vec3d d = to.subtract(from);
        double len = d.length();
        if (len < 1.0E-4) return;
        Vec3d dir = d.multiply(1.0 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3d pt = from.add(dir.multiply(t));
            w.spawnParticles(p, pt.x, pt.y, pt.z, perPoint, spread, spread, spread, 0.0);
        }
    }

    public static void ring(ServerWorld w, ParticleEffect p, Vec3d center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points;
            w.spawnParticles(p, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius,
                    1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    /**
     * Per technique: {startYawOffset, startPitchOffset, endYawOffset, endPitchOffset} in degrees.
     * +yaw = player's right. Must match the client combo in KatanaPoses.ATTACK:
     *   0 kesa-giri (upper right -> lower left), 1 yoko-giri (right -> left),
     *   2 gyaku-kesa (lower left -> upper right), 3 tsuki (thrust, handled separately).
     */
    private static final float[][] ARCS = {
            {70f, -28f, -70f, 28f},
            {70f, -2f, -70f, 4f},
            {-70f, 28f, 70f, -28f},
    };

    /**
     * Slash trail in front of the entity for the given combo technique (see ComboTracker).
     * Returns the sampled positions so callers can add extra particles on them.
     */
    public static Vec3d[] swingArc(ServerWorld w, LivingEntity e, ParticleEffect p, int points, double radius,
                                   int technique) {
        Vec3d[] out = new Vec3d[points];
        Vec3d eye = e.getEyePos().add(0, -0.25, 0);
        for (int i = 0; i < points; i++) {
            double frac = points == 1 ? 0.5 : i / (double) (points - 1);
            Vec3d pt;
            if (technique == 3) {
                // Thrust: straight line forward.
                pt = eye.add(Vec3d.fromPolar(e.getPitch(), e.getYaw()).multiply(0.9 + frac * (radius + 1.0)));
            } else {
                float[] a = ARCS[Math.floorMod(technique, ARCS.length)];
                float yaw = e.getYaw() + a[0] + (float) ((a[2] - a[0]) * frac);
                float pitch = e.getPitch() + a[1] + (float) ((a[3] - a[1]) * frac);
                pt = eye.add(Vec3d.fromPolar(pitch, yaw).multiply(radius));
            }
            out[i] = pt;
            w.spawnParticles(p, pt.x, pt.y, pt.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        return out;
    }

    public static void sound(ServerWorld w, Vec3d pos, SoundEvent s, float volume, float pitch) {
        w.playSound(null, pos.x, pos.y, pos.z, s, SoundCategory.PLAYERS, volume, pitch);
    }
}
