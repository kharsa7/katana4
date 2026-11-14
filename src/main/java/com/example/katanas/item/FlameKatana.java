package com.example.katanas.item;

import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Flame Katana (Tier 1).
 *  Passive: sets targets on fire for 8s (Fire Aspect I is 4s).
 *  Active : crescent slash wave, 14 blocks, ignites + damages everything it passes through.
 */
public class FlameKatana extends KatanaItem {
    private static final double WAVE_RANGE = 14.0;

    public FlameKatana() {
        super(KatanaMaterial.TIER_1, Rarity.RARE, false, 120, 3);
    }

    @Override
    protected void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setOnFireFor(8);
        Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
        KatanaFX.burst(world, ParticleTypes.FLAME, mid, 18, 0.3, 0.06);
        KatanaFX.burst(world, ParticleTypes.LAVA, mid, 4, 0.2, 0.0);
    }

    @Override
    protected boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack) {
        Vec3d origin = player.getEyePos().add(0, -0.3, 0);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();

        Set<UUID> alreadyHit = new HashSet<>();

        for (double d = 1.0; d <= WAVE_RANGE; d += 0.5) {
            Vec3d c = origin.add(look.multiply(d));
            BlockPos bp = BlockPos.ofFloored(c);
            if (!world.getBlockState(bp).getCollisionShape(world, bp).isEmpty()) {
                KatanaFX.burst(world, ParticleTypes.LARGE_SMOKE, c, 6, 0.2, 0.02);
                break; // wave stops at walls
            }

            // Crescent: tips trail behind the centre.
            for (double o = -1.5; o <= 1.5; o += 0.5) {
                Vec3d p = c.add(right.multiply(o)).subtract(look.multiply(o * o * 0.25));
                world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticles(ParticleTypes.SMALL_FLAME, p.x, p.y + 0.1, p.z, 1, 0.08, 0.08, 0.08, 0.01);
            }

            Box hitBox = new Box(c, c).expand(1.6, 1.0, 1.6);
            for (LivingEntity target : AbilityUtil.livingNear(world, player, hitBox)) {
                if (!alreadyHit.add(target.getUuid())) continue;
                target.setOnFireFor(8);
                AbilityUtil.hurtAttack(world, player, target, 7.0f);
                KatanaFX.burst(world, ParticleTypes.FLAME, target.getPos().add(0, target.getHeight() * 0.5, 0), 25, 0.4, 0.08);
            }
        }

        KatanaFX.sound(world, origin, SoundEvents.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        KatanaFX.sound(world, origin, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
        return true;
    }

    @Override
    protected void ambientParticles(ServerWorld world, LivingEntity holder) {
        KatanaFX.sprinkle(world, ParticleTypes.SMALL_FLAME, holder, 1, 0.01);
    }

    @Override
    protected void swingParticles(ServerWorld world, LivingEntity holder, int technique) {
        Vec3d[] arc = KatanaFX.swingArc(world, holder, ParticleTypes.FLAME, 16, 1.8, technique);
        for (int i = 0; i < arc.length; i += 2) {
            world.spawnParticles(ParticleTypes.SMALL_FLAME, arc[i].x, arc[i].y, arc[i].z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
