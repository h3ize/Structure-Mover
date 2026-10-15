package com.example.structuremover;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StructureMover.MODID);

    /** 10 uses; enchantable (unbreaking, mending) via the enchantable/durability item tag in data/. */
    public static final DeferredItem<MoverWandItem> MOVER_WAND =
            ITEMS.register("mover_wand", () -> new MoverWandItem(new Item.Properties().durability(10).rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MOVER_WAND);
        }
    }

    private ModItems() {}
}
