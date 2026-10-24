package com.desastres.disaster;

import com.desastres.config.DisasterConfig;
import com.desastres.entity.DisasterVisualEntity;
import com.desastres.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Clase base de todos los desastres. Se ejecutan en el tick del servidor. */
public abstract class Disaster {
    protected final ServerLevel level;
    protected final Vec3 pos;
    protected final int intensity;
    protected final RandomSource random = RandomSource.create();
    /** Si el desastre puede modificar bloques (config blockDamage). */
    protected final boolean grief = DisasterConfig.BLOCK_DAMAGE.get();
    protected int age = 0;
    /** Entidad 3D que representa visualmente este desastre (puede ser null). */
    protected DisasterVisualEntity visual;
    private boolean finished = false;

    protected Disaster(ServerLevel level, Vec3 pos, int intensity) {
        this.level = level;
        this.pos = pos;
        this.intensity = intensity;
    }

    final void doTick() {
        if (finished) {
            return;
        }
        tick();
        age++;
    }

    protected abstract void tick();

    /** Limpieza al terminar (tambien se llama si se detiene con el boton / comando). */
    protected void onEnd() {}

    public void finish() {
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /**
     * Crea (si hace falta) y actualiza la entidad 3D del desastre.
     * s1..s3 son parametros de tamano que interpreta el renderizador segun el tipo; (dx,dy,dz) una direccion.
     */
    protected void showVisual(DisasterVisualEntity.Kind kind, double x, double y, double z,
                              float s1, float s2, float s3, double dx, double dy, double dz) {
        if (visual == null || visual.isRemoved()) {
            if (!loaded(new BlockPos(Mth.floor(x), 0, Mth.floor(z)))) {
                return;
            }
            DisasterVisualEntity entity = ModEntities.VISUAL.get().create(level);
            if (entity == null) {
                return;
            }
            entity.setKind(kind);
            entity.setPos(x, y, z);
            entity.setParams(s1, s2, s3);
            entity.setDirection(dx, dy, dz);
            level.addFreshEntity(entity);
            visual = entity;
            return;
        }
        visual.setPos(x, y, z);
        visual.setParams(s1, s2, s3);
        visual.setDirection(dx, dy, dz);
    }

    final void cleanupVisual() {
        if (visual != null) {
            visual.discard();
            visual = null;
        }
    }

    protected int scaled(int ticks) {
        return Math.max(20, (int) (ticks * DisasterConfig.DURATION_MULTIPLIER.get()));
    }

    protected boolean loaded(BlockPos p) {
        return level.hasChunkAt(p);
    }

    protected boolean loaded(int x, int z) {
        return level.hasChunkAt(new BlockPos(x, 0, z));
    }

    /** Primer Y libre sobre el suelo (ignora hojas). Solo llamar si la chunk esta cargada. */
    protected int surface(int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
