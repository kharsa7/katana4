package com.example.katanas.item;

import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Frost Katana (Tier 1).
 *  Passive: Slowness + freezing ticks on hit.
 *  Active : 7-block frost cone that locks enemies in place for 4 seconds.
 */
public class FrostKatana extends KatanaItem {
    private static final double CONE_RANGE = 7.0;
    private static final double CONE_MIN_DOT = 0.6; // ~53 degrees half-angle

    public FrostKatana() {
        super(KatanaMaterial.TIER_1, Rarity.RARE, false, 200, 3);
    }

    @Override
    protected void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1), attacker);
        target.setFrozenTicks(Math.max(target.getFrozenTicks(), 200));

        Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
        KatanaFX.burst(world, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()),
                mid, 20, 0.3, 0.1);
        KatanaFX.burst(world, ParticleTypes.SNOWFLAKE, mid, 15, 0.4, 0.05);
        KatanaFX.sound(world, mid, SoundEvents.BLOCK_GLASS_BREAK, 0.6f, 1.6f);
    }

    @Override
    protected boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Box box = player.getBoundingBox().stretch(look.multiply(CONE_RANGE)).expand(2.0);

        for (LivingEntity target : AbilityUtil.livingNear(world, player, box)) {
            Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(eye);
            double dist = toTarget.length();
            if (dist > CONE_RANGE + target.getWidth() || dist < 1.0E-4) continue;
            if (toTarget.multiply(1.0 / dist).dotProduct(look) < CONE_MIN_DOT) continue;

            // Slowness 7 (amplifier 6) drops movement speed to zero for 4 seconds.
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 6), player);
            target.setFrozenTicks(300);
            target.setVelocity(Vec3d.ZERO);
            target.velocityModified = true;
            AbilityUtil.hurt(target, world.getDamageSources().freeze(), 4.0f);

            KatanaFX.burst(world, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()),
                    target.getPos().add(0, target.getHeight() * 0.5, 0), 25, 0.4, 0.1);
        }

        // Visual cone
        Vec3d origin = eye.add(0, -0.3, 0);
        for (double d = 0.5; d <= CONE_RANGE; d += 0.5) {
            Vec3d c = origin.add(look.multiply(d));
            double spread = 0.15 + d * 0.35;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, c.x, c.y, c.z, 6, spread, spread * 0.6, spread, 0.02);
        }
        KatanaFX.sound(world, eye, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
        KatanaFX.sound(world, eye, SoundEvents.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
        return true;
    }

    @Override
    protected void ambientParticles(ServerWorld world, LivingEntity holder) {
        KatanaFX.sprinkle(world, ParticleTypes.SNOWFLAKE, holder, 1, 0.01);
    }

    @Override
    protected void swingParticles(ServerWorld world, LivingEntity holder, int technique) {
        Vec3d[] arc = KatanaFX.swingArc(world, holder, ParticleTypes.SNOWFLAKE, 14, 1.8, technique);
        for (int i = 0; i < arc.length; i += 3) {
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, arc[i].x, arc[i].y, arc[i].z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
