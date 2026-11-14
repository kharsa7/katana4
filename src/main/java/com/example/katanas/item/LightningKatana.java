package com.example.katanas.item;

import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;

/**
 * Lightning Katana (Tier 1).
 *  Passive: 25% chance to shock for 4 bonus magic damage.
 *  Active : calls a lightning bolt on the targeted block/entity (32 blocks).
 *           Wielder gets fire resistance (10s) and full damage resistance (3s) so the bolt is safe.
 */
public class LightningKatana extends KatanaItem {
    private static final double STRIKE_RANGE = 32.0;
    private static final float SHOCK_CHANCE = 0.25f;

    public LightningKatana() {
        super(KatanaMaterial.TIER_1, Rarity.RARE, false, 240, 3);
    }

    @Override
    protected void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (world.random.nextFloat() >= SHOCK_CHANCE) return;

        if (attacker instanceof PlayerEntity player) {
            AbilityUtil.hurtMagic(world, player, target, 4.0f);
        } else {
            AbilityUtil.hurt(target, world.getDamageSources().magic(), 4.0f);
        }
        Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
        KatanaFX.burst(world, ParticleTypes.ELECTRIC_SPARK, mid, 30, 0.4, 0.4);
        KatanaFX.sound(world, mid, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);
    }

    @Override
    protected boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack) {
        AbilityUtil.RayResult ray = AbilityUtil.rayTarget(player, STRIKE_RANGE);
        if (!ray.hit()) {
            player.sendMessage(Text.translatable("message.elementalkatanas.no_target"), true);
            return false;
        }

        // Protect the wielder first (bolt damage + fire).
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4));

        Vec3d pos = ray.pos();
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null) return false;
        bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            bolt.setChanneler(serverPlayer);
        }
        world.spawnEntity(bolt);

        KatanaFX.burst(world, ParticleTypes.ELECTRIC_SPARK, pos.add(0, 1, 0), 60, 1.0, 0.5);
        return true;
    }

    @Override
    protected void ambientParticles(ServerWorld world, LivingEntity holder) {
        KatanaFX.sprinkle(world, ParticleTypes.ELECTRIC_SPARK, holder, 2, 0.05);
    }

    @Override
    protected void swingParticles(ServerWorld world, LivingEntity holder, int technique) {
        KatanaFX.swingArc(world, holder, ParticleTypes.ELECTRIC_SPARK, 18, 1.8, technique);
    }
}
