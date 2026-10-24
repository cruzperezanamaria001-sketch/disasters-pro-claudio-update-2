package com.desastres.registry;

import com.desastres.DisastersMod;
import com.desastres.item.DisasterControllerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DisastersMod.MODID);

    public static final RegistryObject<Item> CONTROLLER = ITEMS.register("disaster_controller",
            () -> new DisasterControllerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {}
}
