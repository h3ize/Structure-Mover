package com.example.structuremover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A scanned building. Stored server-side per player; bounds are mirrored onto the wand stack for the client preview. */
public final class Selection {
    public static final String TAG = "StructureMover";

    public final ResourceKey<Level> dimension;
    public final List<BlockPos> positions;
    public final BlockPos min;
    public final BlockPos max;
    /** Bottom-centre of the bounding box. This is the point that lands on the clicked spot. */
    public final BlockPos anchor;
    public int rotation = 0;

    public Selection(ResourceKey<Level> dimension, List<BlockPos> positions) {
        this.dimension = dimension;
        this.positions = positions;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : positions) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);
        this.anchor = new BlockPos((minX + maxX) >> 1, minY, (minZ + maxZ) >> 1);
    }

    /** Blocks with at least one open side (what you can actually see), thinned evenly to at most {@code cap} entries. */
    public long[] previewShell(int cap) {
        Set<Long> all = new HashSet<>(positions.size() * 2);
        for (BlockPos p : positions) all.add(p.asLong());
        long[] out = new long[positions.size()];
        int n = 0;
        for (BlockPos p : positions) {
            int x = p.getX(), y = p.getY(), z = p.getZ();
            if (!all.contains(BlockPos.asLong(x + 1, y, z)) || !all.contains(BlockPos.asLong(x - 1, y, z))
                    || !all.contains(BlockPos.asLong(x, y + 1, z)) || !all.contains(BlockPos.asLong(x, y - 1, z))
                    || !all.contains(BlockPos.asLong(x, y, z + 1)) || !all.contains(BlockPos.asLong(x, y, z - 1))) {
                out[n++] = p.asLong();
            }
        }
        if (n <= cap) return java.util.Arrays.copyOf(out, n);
        int stride = (n + cap - 1) / cap;
        long[] thinned = new long[(n + stride - 1) / stride];
        for (int i = 0, j = 0; i < n; i += stride) thinned[j++] = out[i];
        return thinned;
    }

    public void writeTo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag t = new CompoundTag();
            t.putInt("minX", min.getX());
            t.putInt("minY", min.getY());
            t.putInt("minZ", min.getZ());
            t.putInt("maxX", max.getX());
            t.putInt("maxY", max.getY());
            t.putInt("maxZ", max.getZ());
            t.putInt("ax", anchor.getX());
            t.putInt("ay", anchor.getY());
            t.putInt("az", anchor.getZ());
            t.putInt("rot", rotation);
            t.putInt("count", positions.size());
            root.put(TAG, t);
        });
    }

    public static void clearFrom(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(TAG)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.remove(TAG));
        }
    }
}
