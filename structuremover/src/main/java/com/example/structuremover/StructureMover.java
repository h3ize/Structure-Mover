package com.example.structuremover;

import com.example.structuremover.client.ClientSetup;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(StructureMover.MODID)
public class StructureMover {
    public static final String MODID = "structuremover";

    public StructureMover(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.registerConfigScreen(container);
        }
    }
}
