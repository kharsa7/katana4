package com.example.katanas.item;

import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
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
 * Dimensional / Space Katana (Tier 2, Mythic - enchantment glint always on).
 *  Passive: bypasses armor. Extra "void" damage = base damage * min(60%, 3% per armor point).
 *  Active : Dimensional Blink - teleport up to 12 blocks along your look direction (through walls,
 *           stopping at the farthest free spot) and slash everything between start and end.
 */
public class SpaceKatana extends KatanaItem {
    private static final double BLINK_DISTANCE = 12.0;
    private static final float BLINK_DAMAGE = 14.0f;

    public SpaceKatana() {
        super(KatanaMaterial.TIER_2, Rarity.EPIC, true, 160, 5);
    }

    @Override
    protected void onKatanaHit(ServerWorld world, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float base = (float) attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double armorPierce = Math.min(0.6, target.getArmor() * 0.03);
        float extra = (float) (base * armorPierce);
        if (extra > 0.1f) {
            // Magic damage ignores armor.
            AbilityUtil.hurt(target, world.getDamageSources().indirectMagic(attacker, attacker), extra);
        }

        Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
        KatanaFX.burst(world, ParticleTypes.END_ROD, mid, 25, 0.35, 0.15);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
        KatanaFX.sound(world, mid, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
    }

    @Override
    protected boolean activateAbility(ServerWorld world, PlayerEntity player, ItemStack stack) {
        Vec3d start = player.getPos();
        Vec3d look = player.getRotationVec(1.0f).normalize();

        // Farthest free destination, scanning inward from max distance.
        Vec3d dest = null;
        for (double d = BLINK_DISTANCE; d >= 1.5; d -= 0.5) {
            Vec3d candidate = start.add(look.multiply(d));
            Box moved = player.getBoundingBox().offset(candidate.subtract(start));
            if (world.getBlockState(BlockPos.ofFloored(candidate)).isOf(Blocks.LAVA)) continue;
            if (world.isSpaceEmpty(player, moved)) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) return false; // nowhere to land

        // Spatial rift: damage everything near the start -> end segment.
        Vec3d segment = dest.subtract(start);
        double length = segment.length();
        Vec3d dir = segment.multiply(1.0 / length);
        Set<UUID> hit = new HashSet<>();
        for (double t = 0; t <= length; t += 0.5) {
            Vec3d p = start.add(dir.multiply(t)).add(0, player.getHeight() * 0.5, 0);
            for (LivingEntity target : AbilityUtil.livingNear(world, player, new Box(p, p).expand(1.5))) {
                if (hit.add(target.getUuid())) {
                    AbilityUtil.hurtAttack(world, player, target, BLINK_DAMAGE);
                    Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
                    KatanaFX.burst(world, ParticleTypes.END_ROD, mid, 30, 0.4, 0.2);
                    world.spawnParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
                }
            }
        }

        // Visuals (before the teleport so the trail stays behind).
        Vec3d chest = player.getHeight() > 0 ? new Vec3d(0, player.getHeight() * 0.5, 0) : Vec3d.ZERO;
        KatanaFX.line(world, ParticleTypes.END_ROD, start.add(chest), dest.add(chest), 0.35, 2, 0.08);
        for (double t = 0; t <= length; t += 2.0) {
            Vec3d p = start.add(dir.multiply(t)).add(chest);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        KatanaFX.burst(world, ParticleTypes.REVERSE_PORTAL, start.add(chest), 40, 0.5, 0.2);
        KatanaFX.sound(world, start, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);

        player.requestTeleport(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;

        KatanaFX.burst(world, ParticleTypes.REVERSE_PORTAL, dest.add(chest), 40, 0.5, 0.2);
        KatanaFX.sound(world, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.8f);
        KatanaFX.sound(world, dest, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        return true;
    }

    @Override
    protected void ambientParticles(ServerWorld world, LivingEntity holder) {
        KatanaFX.sprinkle(world, ParticleTypes.END_ROD, holder, 1, 0.005);
    }

    @Override
    protected void swingParticles(ServerWorld world, LivingEntity holder, int technique) {
        Vec3d[] arc = KatanaFX.swingArc(world, holder, ParticleTypes.END_ROD, 24, 1.9, technique);
        for (int i = 4; i < arc.length; i += 8) {
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, arc[i].x, arc[i].y, arc[i].z, 1, 0, 0, 0, 0);
        }
    }
}
