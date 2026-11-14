package com.example.katanas.ability;

import com.example.katanas.util.AbilityUtil;
import com.example.katanas.util.KatanaFX;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Gravity Katana ability: a timed "well" that pulls + lifts nearby mobs toward a point,
 * then slams them to the ground for magic + fall damage.
 *
 * Wells live in a static list and are advanced from END_WORLD_TICK.
 * They are dropped when the server stops (nothing is persisted).
 */
public final class GravityWellManager {
    private static final int PULL_TICKS = 50;      // 2.5 seconds of pulling
    private static final double RADIUS = 7.0;
    private static final float SLAM_MAGIC_DAMAGE = 6.0f;
    private static final float SLAM_FALL_DAMAGE = 6.0f;

    private static final List<Well> WELLS = new ArrayList<>();

    private static final class Well {
        final ServerWorld world;
        final Vec3d center;
        final UUID owner;
        int age;

        Well(ServerWorld world, Vec3d center, UUID owner) {
            this.world = world;
            this.center = center;
            this.owner = owner;
        }
    }

    private GravityWellManager() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(GravityWellManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> WELLS.clear());
    }

    public static void create(ServerWorld world, Vec3d center, PlayerEntity owner) {
        WELLS.add(new Well(world, center, owner.getUuid()));
    }

    private static void tick(ServerWorld world) {
        Iterator<Well> it = WELLS.iterator();
        while (it.hasNext()) {
            Well well = it.next();
            if (well.world != world) continue;

            well.age++;
            if (well.age <= PULL_TICKS) {
                pull(well);
            } else {
                slam(well);
                it.remove();
            }
        }
    }

    private static List<LivingEntity> affected(Well well) {
        PlayerEntity owner = well.world.getPlayerByUuid(well.owner);
        Box box = new Box(well.center, well.center).expand(RADIUS);
        double r2 = RADIUS * RADIUS;
        List<LivingEntity> list = AbilityUtil.livingNear(well.world, owner, box);
        list.removeIf(e -> e.squaredDistanceTo(well.center) > r2);
        return list;
    }

    private static void pull(Well well) {
        ServerWorld world = well.world;
        Vec3d hover = well.center.add(0, 3.0, 0);

        for (LivingEntity e : affected(well)) {
            Vec3d v = hover.subtract(e.getPos()).multiply(0.12);
            if (v.lengthSquared() > 0.36) {
                v = v.normalize().multiply(0.6);
            }
            e.setVelocity(v);
            e.velocityModified = true;
            e.fallDistance = 0.0f;
        }

        if (well.age % 2 == 0) {
            double shrink = 1.0 - (well.age / (double) PULL_TICKS) * 0.8;
            KatanaFX.ring(world, ParticleTypes.PORTAL, well.center.add(0, 0.2, 0), RADIUS * shrink, 28);
            KatanaFX.ring(world, ParticleTypes.REVERSE_PORTAL, well.center.add(0, 1.5, 0), RADIUS * shrink * 0.6, 16);
            KatanaFX.burst(world, ParticleTypes.REVERSE_PORTAL, hover, 12, 0.6, 0.15);
        }
        if (well.age == 1) {
            KatanaFX.sound(world, well.center, SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.6f);
        }
        if (well.age % 10 == 0) {
            KatanaFX.sound(world, well.center, SoundEvents.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
        }
    }

    private static void slam(Well well) {
        ServerWorld world = well.world;
        PlayerEntity owner = world.getPlayerByUuid(well.owner);

        for (LivingEntity e : affected(well)) {
            e.setVelocity(0, -2.0, 0);
            e.velocityModified = true;
            e.fallDistance = 0.0f; // fall damage is applied manually below (avoids i-frame clashes)

            AbilityUtil.hurt(e, world.getDamageSources().indirectMagic(owner, owner), SLAM_MAGIC_DAMAGE);
            AbilityUtil.hurt(e, world.getDamageSources().fall(), SLAM_FALL_DAMAGE);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2), owner);
        }

        KatanaFX.ring(world, ParticleTypes.POOF, well.center.add(0, 0.2, 0), 2.0, 20);
        KatanaFX.ring(world, ParticleTypes.POOF, well.center.add(0, 0.2, 0), 4.0, 32);
        KatanaFX.ring(world, ParticleTypes.POOF, well.center.add(0, 0.2, 0), RADIUS, 48);
        KatanaFX.sound(world, well.center, SoundEvents.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
        KatanaFX.sound(world, well.center, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.4f);
    }
}
