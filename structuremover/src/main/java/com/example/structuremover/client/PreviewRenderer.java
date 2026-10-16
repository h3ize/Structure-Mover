package com.example.structuremover.client;

import com.example.structuremover.MoverWandItem;
import com.example.structuremover.Selection;
import com.example.structuremover.StructureMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import com.example.structuremover.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws a green box around the selected building and an orange ghost box where it will land. */
@EventBusSubscriber(modid = com.example.structuremover.StructureMover.MODID, value = Dist.CLIENT)
public final class PreviewRenderer {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MoverWandItem)) stack = mc.player.getOffhandItem();
        if (!(stack.getItem() instanceof MoverWandItem)) return;

        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.contains(Selection.TAG)) return;
        CompoundTag d = cd.copyTag().getCompound(Selection.TAG);

        int minX = d.getInt("minX"), minY = d.getInt("minY"), minZ = d.getInt("minZ");
        int maxX = d.getInt("maxX"), maxY = d.getInt("maxY"), maxZ = d.getInt("maxZ");
        int ax = d.getInt("ax"), ay = d.getInt("ay"), az = d.getInt("az");
        int rot = d.getInt("rot");

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());

        // current selection
        AABB src = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).move(-cam.x, -cam.y, -cam.z);
        float pulse = 0.65F + 0.35F * (float) Math.sin((System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2.0);
        LevelRenderer.renderLineBox(ps, vc, src, 0.65F, 0.4F, 1.0F, pulse);

        // destination ghost
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos t = bhr.getBlockPos().relative(bhr.getDirection());
            int rMinX = Integer.MAX_VALUE, rMaxX = Integer.MIN_VALUE, rMinZ = Integer.MAX_VALUE, rMaxZ = Integer.MIN_VALUE;
            for (int x : new int[] { minX, maxX }) {
                for (int z : new int[] { minZ, maxZ }) {
                    int[] r = StructureMath.rotate(x - ax, z - az, rot);
                    rMinX = Math.min(rMinX, r[0]);
                    rMaxX = Math.max(rMaxX, r[0]);
                    rMinZ = Math.min(rMinZ, r[1]);
                    rMaxZ = Math.max(rMaxZ, r[1]);
                }
            }
            AABB ghost = new AABB(
                    t.getX() + rMinX, t.getY() + (minY - ay), t.getZ() + rMinZ,
                    t.getX() + rMaxX + 1, t.getY() + (maxY - ay) + 1, t.getZ() + rMaxZ + 1)
                    .move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(ps, vc, ghost, 1.0F, 0.75F, 0.2F, pulse);
            buffers.endBatch(RenderType.lines());

            if (Config.PREVIEW_BLOCKS.get()) renderGhostBlocks(mc, ps, cam, buffers, t, ax, ay, az, rot);
        }

        buffers.endBatch(RenderType.lines());
    }

    /** Draws the selected building's visible blocks, rotated and offset to the aimed spot, as see-through blocks. */
    private static void renderGhostBlocks(Minecraft mc, PoseStack ps, Vec3 cam, MultiBufferSource.BufferSource buffers,
                                          BlockPos target, int ax, int ay, int az, int rot) {
        long[] shell = ClientSelectionCache.get();
        if (shell.length == 0) return;

        int max = Config.PREVIEW_MAX_BLOCKS.get();
        int stride = Math.max(1, (shell.length + max - 1) / max);
        float alpha = Config.PREVIEW_OPACITY.get().floatValue();
        Rotation rotation = StructureMath.toRotation(rot);
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        GhostVertexConsumer ghost = new GhostVertexConsumer(buffers.getBuffer(RenderType.translucent()), alpha);
        MultiBufferSource ghostSource = type -> ghost;

        for (int i = 0; i < shell.length; i += stride) {
            BlockPos src = BlockPos.of(shell[i]);
            BlockState state = mc.level.getBlockState(src);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            int[] r = StructureMath.rotate(src.getX() - ax, src.getZ() - az, rot);
            double dx = target.getX() + r[0] - cam.x;
            double dy = target.getY() + (src.getY() - ay) - cam.y;
            double dz = target.getZ() + r[1] - cam.z;

            ps.pushPose();
            ps.translate(dx, dy, dz);
            dispatcher.renderSingleBlock(state.rotate(rotation), ps, ghostSource,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        buffers.endBatch(RenderType.translucent());
    }

    private PreviewRenderer() {}
}
