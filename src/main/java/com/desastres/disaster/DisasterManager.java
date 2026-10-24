package com.desastres.disaster;

import com.desastres.DisastersMod;
import com.desastres.config.DisasterConfig;
import com.desastres.disaster.impl.MeteorDisaster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Mantiene la lista de desastres activos y los ticka cada tick del servidor. */
@Mod.EventBusSubscriber(modid = DisastersMod.MODID)
public final class DisasterManager {
    private static final List<Disaster> ACTIVE = new ArrayList<>();
    private static final List<Disaster> PENDING = new ArrayList<>();

    private DisasterManager() {}

    /** Anade un desastre (se activa en el siguiente tick). Solo desde el hilo del servidor. */
    public static void add(Disaster disaster) {
        PENDING.add(disaster);
    }

    public static boolean hasActive() {
        return !ACTIVE.isEmpty() || !PENDING.isEmpty();
    }

    public static boolean canUse(ServerPlayer player) {
        if (!DisasterConfig.REQUIRE_PERMISSION.get()) {
            return true;
        }
        return player.isCreative() || player.hasPermissions(2);
    }

    private static int countMajor() {
        int n = 0;
        for (Disaster d : ACTIVE) {
            if (!(d instanceof MeteorDisaster) && !d.isFinished()) n++;
        }
        for (Disaster d : PENDING) {
            if (!(d instanceof MeteorDisaster) && !d.isFinished()) n++;
        }
        return n;
    }

    /**
     * Crea un desastre.
     * @param targetMode 0 = donde mira el jugador, 1 = sobre el jugador
     */
    public static boolean spawn(ServerPlayer player, DisasterType type, int intensity, int targetMode) {
        if (!canUse(player)) {
            player.displayClientMessage(Component.translatable("message.disasters.no_permission"), true);
            return false;
        }
        if (countMajor() >= DisasterConfig.MAX_ACTIVE.get()) {
            player.displayClientMessage(Component.translatable("message.disasters.too_many"), true);
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 target;
        if (targetMode == 1) {
            target = player.position();
        } else {
            HitResult hit = player.pick(160.0D, 1.0F, false);
            if (hit.getType() == HitResult.Type.MISS) {
                double x = player.getX() + look.x * 24.0D;
                double z = player.getZ() + look.z * 24.0D;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
                target = new Vec3(x, y, z);
            } else {
                target = hit.getLocation();
            }
        }

        if (!level.hasChunkAt(BlockPos.containing(target))) {
            player.displayClientMessage(Component.translatable("message.disasters.unloaded"), true);
            return false;
        }

        Disaster disaster = type.create(level, target, look, intensity);
        PENDING.add(disaster);
        player.displayClientMessage(Component.translatable("message.disasters.spawned",
                Component.translatable(type.translationKey()), intensity), true);
        return true;
    }

    /** Detiene todos los desastres activos. Devuelve cuantos habia. */
    public static int stopAll() {
        int n = 0;
        for (Disaster d : ACTIVE) {
            if (!d.isFinished()) {
                d.finish();
                n++;
            }
        }
        for (Disaster d : PENDING) {
            if (!d.isFinished()) {
                d.finish();
                n++;
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Disaster> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Disaster d = it.next();
            try {
                d.doTick();
            } catch (Exception ex) {
                DisastersMod.LOGGER.error("Error en el desastre {}", d.getClass().getSimpleName(), ex);
                d.finish();
            }
            if (d.isFinished()) {
                try {
                    d.onEnd();
                } catch (Exception ex) {
                    DisastersMod.LOGGER.error("Error al limpiar el desastre {}", d.getClass().getSimpleName(), ex);
                }
                d.cleanupVisual();
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        PENDING.clear();
    }
}
