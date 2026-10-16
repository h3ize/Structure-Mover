package com.example.structuremover;

import com.example.structuremover.client.ClientSelectionCache;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StructureMover.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Network {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(SelectionSyncPayload.TYPE, SelectionSyncPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientSelectionCache.set(payload.shell())));
    }

    private Network() {}
}
