package com.example.structuremover;

import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = StructureMover.MODID)
public final class MoverCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("structuremover")
                .then(Commands.literal("undo").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MoveService.Result r = MoveService.undo(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(r.message()), false);
                    return r.ok() ? Command.SINGLE_SUCCESS : 0;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    SelectionManager.remove(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Selection cleared. (Preview vanishes on your next wand click.)"), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private MoverCommands() {}
}
