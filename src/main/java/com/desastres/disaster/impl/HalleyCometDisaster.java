package com.desastres.disaster.impl;

import com.desastres.disaster.CometIceTracker;
import com.desastres.disaster.Disaster;
import com.desastres.disaster.Fx;
import com.desastres.entity.DisasterVisualEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A bright Halley-like comet that flies in and leaves tracked ice at impact. */
public final class HalleyCometDisaster extends Disaster {
    private Vec3 current;
    private final Vec3 velocity;
    private final float impactPower;
    private boolean impacted;

    public HalleyCometDisaster(ServerLevel level, Vec3 target, int intensity) {
        super(level, target, intensity);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 70.0 + random.nextDouble() * 45.0;
        double startY = Math.min(level.getMaxBuildHeight() - 8, Math.max(target.y + 90.0, 150.0));
        this.current = new Vec3(target.x + Math.cos(angle) * distance, startY, target.z + Math.sin(angle) * distance);
        this.velocity = target.subtract(current).normalize().scale(1.25 + 0.15 * intensity);
        this.impactPower = 3.0F + intensity * 1.4F;
    }

    @Override protected void tick() {
        if (impacted) { finish(); return; }
        if (age > 900 || current.y <= level.getMinBuildHeight() + 2) { impact(); return; }
        int steps = Math.max(1, (int)Math.ceil(velocity.length() * 2.0));
        Vec3 step = velocity.scale(1.0 / steps);
        for (int i=0; i<steps; i++) {
            current = current.add(step);
            if (current.y <= level.getMinBuildHeight() + 2) { impact(); return; }
            BlockPos bp = BlockPos.containing(current);
            if (!level.isOutsideBuildHeight(bp) && loaded(bp)) {
                BlockState state = level.getBlockState(bp);
                if (!state.getCollisionShape(level, bp).isEmpty() || !state.getFluidState().isEmpty()) { impact(); return; }
            }
        }
        Vec3 d = velocity.normalize();
        showVisual(DisasterVisualEntity.Kind.HALLEY_COMET, current.x, current.y, current.z,
                1.4F + intensity * 0.25F, 0, 0, d.x, d.y, d.z);
        if (age % 2 == 0) {
            Fx.burst(level, ParticleTypes.END_ROD, current.x, current.y, current.z, 4, 0.7, 0.7, 0.7, 0.025);
            Fx.burst(level, ParticleTypes.SOUL_FIRE_FLAME, current.x - d.x * 2, current.y - d.y * 2, current.z - d.z * 2, 5, 0.8, 0.8, 0.8, 0.01);
            Fx.burst(level, ParticleTypes.CLOUD, current.x - d.x * 4, current.y - d.y * 4, current.z - d.z * 4, 2, 0.6, 0.6, 0.6, 0.005);
        }
        if (age % 12 == 0) Fx.sound(level, current.x, current.y, current.z, SoundEvents.FIRECHARGE_USE, 2.5F, 0.65F);
    }

    private void impact() {
        if (impacted) return;
        impacted = true;
        BlockPos center = BlockPos.containing(current.x, Math.max(level.getMinBuildHeight()+2, current.y), current.z);
        // Land the impact on the actual surface at the target's X/Z when available.
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        Vec3 p = new Vec3(center.getX()+0.5, Math.max(surfaceY, center.getY()), center.getZ()+0.5);
        Level.ExplosionInteraction mode = grief ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level.explode(null, p.x, p.y, p.z, impactPower, grief, mode);
        Fx.burst(level, ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 2, 1, 1, 1, 0);
        Fx.burst(level, ParticleTypes.SNOWFLAKE, p.x, p.y+1, p.z, 80, impactPower*0.55, 2, impactPower*0.55, 0.03);
        Fx.burst(level, ParticleTypes.END_ROD, p.x, p.y+2, p.z, 35, impactPower*0.35, impactPower*0.3, impactPower*0.35, 0.02);
        AABB area = new AABB(p, p).inflate(impactPower * 2.2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            Vec3 away = entity.position().subtract(p).normalize().scale(1.4);
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, 0.7, away.z));
            entity.hurtMarked = true;
            entity.hurt(level.damageSources().explosion(null), 4.0F + intensity * 2.0F);
            entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + 100, entity.getTicksRequiredToFreeze()));
        }
        if (grief) placeIce(p);
        Fx.sound(level, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, 5.0F, 0.55F);
        finish();
    }

    private void placeIce(Vec3 p) {
        BlockPos c = BlockPos.containing(p);
        int r = Math.min(5, 2 + intensity);
        for (int dx=-r; dx<=r; dx++) for (int dz=-r; dz<=r; dz++) for (int dy=-1; dy<=2; dy++) {
            double dist = dx*dx + dz*dz + dy*dy*1.5;
            if (dist > r*r || random.nextFloat() > 0.78F) continue;
            BlockPos bp = c.offset(dx, dy, dz);
            if (level.isOutsideBuildHeight(bp) || !loaded(bp)) continue;
            BlockState old = level.getBlockState(bp);
            // Only replace air or replaceable non-fluid blocks; never overwrite player-built solid blocks.
            if (!(old.isAir() || (old.canBeReplaced() && old.getFluidState().isEmpty()))) continue;
            if (level.setBlock(bp, Blocks.ICE.defaultBlockState(), 3)) CometIceTracker.track(level, bp);
        }
    }
}
