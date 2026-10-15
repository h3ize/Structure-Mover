package com.example.structuremover.client;

import com.example.structuremover.ModItems;
import com.example.structuremover.MoverWandItem;
import com.example.structuremover.StructureMover;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Switches the wand to its glowing model while a building is selected. */
@EventBusSubscriber(modid = StructureMover.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.MOVER_WAND.get(),
                ResourceLocation.fromNamespaceAndPath(StructureMover.MODID, "active"),
                (stack, level, entity, seed) -> MoverWandItem.hasSelection(stack) ? 1.0F : 0.0F));
    }

    private ClientSetup() {}
}
