package com.example.katanas.client;

/**
 * Offset (blocks) + rotation (degrees) applied to the held katana in first person.
 * The pivot is the hand, so rotations swing the blade around the grip.
 *
 * Axes (right-handed player, first person): +x = right, +y = up, -z = forward.
 *  rx = pitch (positive tips the blade forward/down), ry = yaw (positive turns the tip to the left),
 *  rz = roll around the forward axis (positive tips the blade clockwise).
 * For the left hand ry/rz and tx are mirrored automatically.
 */
public record Pose(float tx, float ty, float tz, float rx, float ry, float rz) {
    public static final Pose ZERO = new Pose(0, 0, 0, 0, 0, 0);

    public Pose add(Pose o) {
        return new Pose(tx + o.tx, ty + o.ty, tz + o.tz, rx + o.rx, ry + o.ry, rz + o.rz);
    }

    public static Pose lerp(Pose a, Pose b, float t) {
        return new Pose(
                a.tx + (b.tx - a.tx) * t,
                a.ty + (b.ty - a.ty) * t,
                a.tz + (b.tz - a.tz) * t,
                a.rx + (b.rx - a.rx) * t,
                a.ry + (b.ry - a.ry) * t,
                a.rz + (b.rz - a.rz) * t);
    }
}
