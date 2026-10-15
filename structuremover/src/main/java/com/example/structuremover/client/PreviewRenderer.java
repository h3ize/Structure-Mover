package com.example.structuremover.client;

import com.example.structuremover.MoverWandItem;
import com.example.structuremover.Selection;
import com.example.structuremover.StructureMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
        }

        buffers.endBatch(RenderType.lines());
    }

    private PreviewRenderer() {}
}
