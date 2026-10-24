package com.desastres.registry;

import com.desastres.DisastersMod;
import com.desastres.entity.DisasterVisualEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DisastersMod.MODID);

    public static final RegistryObject<EntityType<DisasterVisualEntity>> VISUAL = ENTITIES.register("disaster_visual",
            () -> EntityType.Builder.<DisasterVisualEntity>of(DisasterVisualEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("disaster_visual"));

    private ModEntities() {}
}
