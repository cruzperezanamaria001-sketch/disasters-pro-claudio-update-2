package com.desastres.disaster.impl;

import com.desastres.disaster.Disaster;
import com.desastres.disaster.Fx;
import com.desastres.entity.DisasterVisualEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Una pared de agua que avanza en la direccion en la que mira el jugador, siguiendo el terreno. */
public class TsunamiDisaster extends Disaster {
    private static final int BODY_LEN = 5;
    private static final double SPEED = 0.7D;

    private final Vec3 dir;
    private final Vec3 side;
    private final int halfWidth;
    private final int height;
    private final int range;

    private double front = 0.0D;
    private int lastStep = -1;
    private final Deque<List<BlockPos>> slices = new ArrayDeque<>();

    public TsunamiDisaster(ServerLevel level, Vec3 start, int intensity, Vec3 look) {
        super(level, start, intensity);
        double lx = look.x;
        double lz = look.z;
        double len = Math.sqrt(lx * lx + lz * lz);
        if (len < 1.0E-4D) {
            lx = 1.0D;
            lz = 0.0D;
            len = 1.0D;
        }
        this.dir = new Vec3(lx / len, 0.0D, lz / len);
        this.side = new Vec3(-dir.z, 0.0D, dir.x);
        this.halfWidth = 14 + 8 * intensity;
        this.height = 5 + 3 * intensity;
        this.range = 110 + 60 * intensity;
        Fx.sound(level, start.x, start.y, start.z, SoundEvents.AMBIENT_UNDERWATER_ENTER, 8.0F, 0.4F);
    }

    @Override
    protected void tick() {
        if (front > range) {
            finish();
            return;
        }
        front += SPEED;
        int step = (int) front;
        while (lastStep < step) {
            lastStep++;
            makeSlice(lastStep);
        }
        pushEntities();
        effects();
        updateVisual();
    }

    private void updateVisual() {
        double vx = pos.x + dir.x * (front - 1.0D);
        double vz = pos.z + dir.z * (front - 1.0D);
        int ix = Mth.floor(vx);
        int iz = Mth.floor(vz);
        if (!loaded(ix, iz)) {
            return;
        }
        showVisual(DisasterVisualEntity.Kind.TSUNAMI, vx, surface(ix, iz), vz,
                halfWidth, height * 1.15F, BODY_LEN, dir.x, 0.0D, dir.z);
    }

    private void makeSlice(int s) {
        List<BlockPos> placed = new ArrayList<>();
        if (grief) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                int x = Mth.floor(pos.x + dir.x * s + side.x * w);
                int z = Mth.floor(pos.z + dir.z * s + side.z * w);
                if (!loaded(x, z)) {
                    continue;
                }
                int gy = surface(x, z);
                for (int y = gy; y < gy + height; y++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(bp);
                    if (st.isAir() || (st.canBeReplaced() && st.getFluidState().isEmpty())) {
                        level.setBlock(bp, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                        placed.add(bp);
                    }
                }
            }
        }
        slices.addLast(placed);
        while (slices.size() > BODY_LEN) {
            clear(slices.pollFirst());
        }
    }

    private void clear(List<BlockPos> slice) {
        if (slice == null) {
            return;
        }
        for (BlockPos bp : slice) {
            if (loaded(bp) && level.getBlockState(bp).is(Blocks.WATER)) {
                level.setBlock(bp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private void pushEntities() {
        double cx = pos.x + dir.x * (front - BODY_LEN / 2.0D);
        double cz = pos.z + dir.z * (front - BODY_LEN / 2.0D);
        double rad = halfWidth + BODY_LEN + 4;
        AABB box = new AABB(cx - rad, pos.y - 64, cz - rad, cx + rad, pos.y + 96, cz + rad);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
            if (e instanceof DisasterVisualEntity || e.isSpectator()) {
                continue;
            }
            double rx = e.getX() - pos.x;
            double rz = e.getZ() - pos.z;
            double along = rx * dir.x + rz * dir.z;
            double lateral = rx * side.x + rz * side.z;
            if (along > front + 1.5D || along < front - BODY_LEN - 1 || Math.abs(lateral) > halfWidth + 1) {
                continue;
            }
            int gy = surface(e.getBlockX(), e.getBlockZ());
            if (e.getY() > gy + height + 3) {
                continue;
            }
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.4D + dir.x * SPEED * 0.9D,
                    Math.max(v.y, 0.2D),
                    v.z * 0.4D + dir.z * SPEED * 0.9D);
            e.hurtMarked = true;
            if (e instanceof LivingEntity living && e.tickCount % 10 == 0) {
                living.hurt(level.damageSources().drown(), 1.5F + intensity);
            }
        }
    }

    private void effects() {
        double fx = pos.x + dir.x * front;
        double fz = pos.z + dir.z * front;
        for (int i = 0; i < 4; i++) {
            double w = (random.nextDouble() * 2.0D - 1.0D) * halfWidth;
            double px = fx + side.x * w;
            double pz = fz + side.z * w;
            if (!loaded(Mth.floor(px), Mth.floor(pz))) {
                continue;
            }
            int gy = surface(Mth.floor(px), Mth.floor(pz));
            double py = gy + height * (0.6D + random.nextDouble() * 0.5D);
            Fx.burst(level, ParticleTypes.SPLASH, px, py, pz, 8, 1.2D, 1.0D, 1.2D, 0.2D);
            Fx.dir(level, ParticleTypes.CLOUD, px, py, pz, dir.x * 0.3D, 0.05D, dir.z * 0.3D);
        }
        if (age % 8 == 0 && loaded(Mth.floor(fx), Mth.floor(fz))) {
            Fx.sound(level, fx, surface(Mth.floor(fx), Mth.floor(fz)), fz, SoundEvents.GENERIC_SPLASH, 6.0F, 0.4F);
        }
    }

    @Override
    protected void onEnd() {
        for (List<BlockPos> slice : slices) {
            clear(slice);
        }
        slices.clear();
    }
}
