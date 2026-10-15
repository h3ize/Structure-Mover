package com.example.structuremover;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = StructureMover.MODID)
public final class SelectionManager {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    public static Selection get(ServerPlayer p) { return SELECTIONS.get(p.getUUID()); }
    public static void put(ServerPlayer p, Selection s) { SELECTIONS.put(p.getUUID(), s); }
    public static void remove(ServerPlayer p) { SELECTIONS.remove(p.getUUID()); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
        MoveService.forget(event.getEntity().getUUID());
    }

    private SelectionManager() {}
}
