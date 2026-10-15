package com.example.structuremover;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

public class MoverWandItem extends Item {
    public MoverWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide || !(ctx.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack stack = ctx.getItemInHand();

        if (!canUse(player)) return InteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            clear(player, stack);
            return InteractionResult.SUCCESS;
        }

        Selection sel = SelectionManager.get(player);
        if (sel != null && !sel.dimension.equals(level.dimension())) {
            clear(player, stack);
            sel = null;
        }

        if (sel == null) {
            Selection.clearFrom(stack); // drop stale preview data
            select(player, serverLevel, stack, ctx.getClickedPos());
        } else {
            BlockPos target = ctx.getClickedPos().relative(ctx.getClickedFace());
            MoveService.Result r = MoveService.move(player, serverLevel, sel, target);
            msg(player, r.message(), r.ok());
            if (r.ok()) {
                SelectionManager.remove(player);
                Selection.clearFrom(stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Right-click in the air: rotate the selection 90 degrees (sneak: cancel it). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!level.isClientSide && p instanceof ServerPlayer player && canUse(player)) {
            if (player.isShiftKeyDown()) {
                clear(player, stack);
            } else {
                Selection sel = SelectionManager.get(player);
                if (sel == null) {
                    msg(player, "Nothing selected. Right-click a block of the building first.", false);
                } else {
                    sel.rotation = (sel.rotation + 1) & 3;
                    sel.writeTo(stack);
                    msg(player, "Rotation: " + (sel.rotation * 90) + " degrees", true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void select(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos start) {
        int max = Config.MAX_BLOCKS.get();
        List<BlockPos> found = StructureScanner.scan(level, start, max);
        if (found == null) {
            msg(player, "Selection is larger than " + max + " blocks. It probably touches terrain or something huge. "
                    + "Raise maxBlocks or add blocks to ignoredBlocks in the config.", false);
            return;
        }
        if (found.isEmpty()) {
            msg(player, "Nothing to select there.", false);
            return;
        }
        Selection sel = new Selection(level.dimension(), found);
        SelectionManager.put(player, sel);
        sel.writeTo(stack);
        msg(player, "Selected " + found.size() + " blocks. Right-click where it should go (R-click air = rotate, sneak+click = cancel).", true);
    }

    private static void clear(ServerPlayer player, ItemStack stack) {
        SelectionManager.remove(player);
        Selection.clearFrom(stack);
        msg(player, "Selection cleared.", true);
    }

    private static boolean canUse(ServerPlayer player) {
        if (!player.getAbilities().mayBuild) return false;
        if (Config.CREATIVE_ONLY.get() && !player.isCreative()) {
            msg(player, "Creative mode is required to use the wand.", false);
            return false;
        }
        return true;
    }

    private static void msg(ServerPlayer player, String text, boolean good) {
        player.displayClientMessage(Component.literal(text).withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        if (!good || text.length() > 60) {
            player.sendSystemMessage(Component.literal("[Structure Mover] " + text));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a block: select the connected building").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click a block again: move it there").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click air: rotate 90 degrees").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + click: cancel").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("/structuremover undo reverts the last move").withStyle(ChatFormatting.DARK_AQUA));
    }
}
