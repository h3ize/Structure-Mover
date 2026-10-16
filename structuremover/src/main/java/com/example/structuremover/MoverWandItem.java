package com.example.structuremover;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class MoverWandItem extends Item {
    public MoverWandItem(Properties properties) {
        super(properties);
    }

    public static boolean hasSelection(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(Selection.TAG);
    }

    /** The crystal glows while something is selected. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || hasSelection(stack);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.AMETHYST_SHARD);
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
                syncPreview(player, null);
                Selection.clearFrom(stack);
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot); // one use per successful move
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Right-click in the air: rotate the selection 90 degrees, or undo the last move if nothing is selected (sneak: cancel). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!level.isClientSide && p instanceof ServerPlayer player && canUse(player)) {
            if (player.isShiftKeyDown()) {
                clear(player, stack);
            } else {
                Selection sel = SelectionManager.get(player);
                if (sel == null) {
                    MoveService.Result r = MoveService.undo(player);
                    msg(player, r.message(), r.ok());
                } else {
                    sel.rotation = (sel.rotation + 1) & 3;
                    sel.writeTo(stack);
                    msg(player, "Rotation: " + (sel.rotation * 90) + " degrees", true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void select(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos clicked) {
        // Clicking a leaf selects its tree; clicking natural ground is refused.
        BlockPos start = StructureScanner.resolveStart(level, clicked);
        BlockState startState = level.getBlockState(start);
        if (Config.IGNORE_TERRAIN.get() && StructureScanner.isTerrain(startState) && !Config.isForceIncluded(startState)) {
            msg(player, "That is natural terrain or plants. Click a block of the building itself.", false);
            return;
        }

        int max = Config.MAX_BLOCKS.get();
        StructureScanner.ScanResult res = StructureScanner.scan(level, start, max);
        if (res.tooBig()) {
            msg(player, "Selection is larger than " + max + " blocks. It probably touches something huge. "
                    + "Raise maxBlocks or add blocks to ignoredBlocks in the config.", false);
            return;
        }
        if (res.positions().isEmpty()) {
            msg(player, "Nothing to select there.", false);
            return;
        }
        Selection sel = new Selection(level.dimension(), res.positions());
        SelectionManager.put(player, sel);
        MoveService.forget(player.getUUID());
        sel.writeTo(stack);
        syncPreview(player, sel);

        StringBuilder sb = new StringBuilder("Selected " + res.positions().size() + " blocks");
        if (res.leaves() > 0) sb.append(" (").append(res.leaves()).append(" leaves)");
        if (res.terrainTouched() > 0) sb.append(", ignored ").append(res.terrainTouched()).append(" terrain blocks");
        sb.append(". Right-click where it should go.");
        msg(player, sb.toString(), true);
    }

    /** Sends the selection's outer blocks to the client so it can draw the transparent placement preview. */
    private static void syncPreview(ServerPlayer player, Selection sel) {
        long[] shell = sel == null ? new long[0] : sel.previewShell(50000);
        PacketDistributor.sendToPlayer(player, new SelectionSyncPayload(shell));
    }

    private static void clear(ServerPlayer player, ItemStack stack) {
        SelectionManager.remove(player);
        syncPreview(player, null);
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
        tooltip.add(Component.literal("Right-click a building: select it (trees take their leaves)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click a block again: move it there (1 use)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click air: rotate 90 degrees").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click air again after a move: undo it").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + click: cancel").withStyle(ChatFormatting.GRAY));
    }
}
