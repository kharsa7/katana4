package com.example.katanas.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

/** Targeting + damage helpers shared by all katana abilities. */
public final class AbilityUtil {
    private AbilityUtil() {}

    /** pos = where the ray ended (entity / block hit, or max range); hit = something was actually hit. */
    public record RayResult(Vec3d pos, boolean hit) {}

    public static RayResult rayTarget(PlayerEntity player, double range) {
        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(range));

        BlockHitResult blockHit = player.getWorld().raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        boolean blockHitSomething = blockHit.getType() != HitResult.Type.MISS;
        Vec3d blockEnd = blockHitSomething ? blockHit.getPos() : end;

        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(player, start, blockEnd, box,
                e -> !e.isSpectator() && e.canHit(), start.squaredDistanceTo(blockEnd));

        if (entityHit != null) {
            return new RayResult(entityHit.getEntity().getPos(), true);
        }
        return new RayResult(blockEnd, blockHitSomething);
    }

    /** Living, alive, non-spectator, non-teammate, non-armor-stand entities inside the box (never the owner). */
    public static List<LivingEntity> livingNear(ServerWorld world, PlayerEntity owner, Box box) {
        return world.getEntitiesByClass(LivingEntity.class, box, e ->
                e != owner && e.isAlive() && !e.isSpectator()
                        && !(e instanceof ArmorStandEntity)
                        && (owner == null || !owner.isTeammate(e)));
    }

    /** Deal damage ignoring the target's i-frames (so ability hits still land right after a normal hit). */
    public static void hurt(LivingEntity target, DamageSource source, float amount) {
        target.timeUntilRegen = 0;
        target.damage(source, amount);
    }

    public static void hurtAttack(ServerWorld w, PlayerEntity p, LivingEntity t, float amount) {
        hurt(t, w.getDamageSources().playerAttack(p), amount);
    }

    public static void hurtMagic(ServerWorld w, PlayerEntity p, LivingEntity t, float amount) {
        hurt(t, w.getDamageSources().indirectMagic(p, p), amount);
    }
}
