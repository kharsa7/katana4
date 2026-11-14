package com.example.katanas.item;

import com.example.katanas.ability.GravityWellManager;
import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;

/**
 * Gravity Katana (Tier 2).
 *  Passive: 1s Levitation + strong knockback on hit.
 *  Active : gravity well at the targeted point (up to 20 blocks). Pulls and lifts mobs for 2.5s,
 *           then slams them down for magic + fall damage. See {@link GravityWellManager}.
 */
public class GravityKatana extends KatanaItem {
    private static final double WELL_RANGE = 20.0;

    public GravityKatana() {
        super(KatanaMaterial.TIER_2, Rarity.EPIC, false, 300, 5);
    }

    @Override
    protected void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 0), attacker);
        target.takeKnockback(0.9, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());

        Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
        KatanaFX.burst(world, ParticleTypes.PORTAL, mid, 30, 0.4, 0.6);
        KatanaFX.sound(world, mid, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.6f);
    }

    @Override
    protected boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack) {
        // Works on a hit point or in mid-air at max range.
        Vec3d center = AbilityUtil.rayTarget(player, WELL_RANGE).pos();
        GravityWellManager.create(world, center, player);
        KatanaFX.burst(world, ParticleTypes.REVERSE_PORTAL, center.add(0, 1, 0), 60, 1.0, 0.3);
        KatanaFX.sound(world, center, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        return true;
    }

    @Override
    protected void ambientParticles(ServerWorld world, LivingEntity holder) {
        KatanaFX.sprinkle(world, ParticleTypes.REVERSE_PORTAL, holder, 1, 0.02);
    }

    @Override
    protected void swingParticles(ServerWorld world, LivingEntity holder, int technique) {
        KatanaFX.swingArc(world, holder, ParticleTypes.PORTAL, 20, 1.8, technique);
    }
}
