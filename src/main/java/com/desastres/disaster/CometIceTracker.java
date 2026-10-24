package com.desastres.disaster;

import com.desastres.DisastersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Tracks only ice blocks placed by a Halley impact; player edits are never overwritten. */
@Mod.EventBusSubscriber(modid = DisastersMod.MODID)
public final class CometIceTracker {
    private static final Map<ServerLevel, List<Entry>> PENDING = new HashMap<>();
    private CometIceTracker() {}
    public static void track(ServerLevel level, BlockPos pos) {
        PENDING.computeIfAbsent(level, k -> new ArrayList<>()).add(new Entry(pos.immutable(), level.getGameTime() + 400L));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Map.Entry<ServerLevel, List<Entry>> world : PENDING.entrySet()) {
            ServerLevel level = world.getKey();
            Iterator<Entry> it = world.getValue().iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (level.getGameTime() < e.due) continue;
                if (!level.hasChunkAt(e.pos)) continue; // retry when chunk becomes loaded
                BlockState current = level.getBlockState(e.pos);
                // Convert only if the exact event-created ice remains there.
                if (current.is(Blocks.ICE)) {
                    level.setBlock(e.pos, Blocks.WATER.defaultBlockState(), 3);
                }
                it.remove();
            }
        }
        PENDING.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PENDING.clear(); }
    private static final class Entry { final BlockPos pos; final long due; Entry(BlockPos pos, long due) { this.pos = pos; this.due = due; } }
}
