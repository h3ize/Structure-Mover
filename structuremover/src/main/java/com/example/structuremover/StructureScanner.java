package com.example.structuremover;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureScanner {
    private static final int[][] FACE = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] ALL = buildAll();

    private static int[][] buildAll() {
        List<int[]> l = new ArrayList<>();
        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                    if (x != 0 || y != 0 || z != 0) l.add(new int[] { x, y, z });
        return l.toArray(new int[0][]);
    }

    /** @return the connected blocks, an empty list if the start is air, or null if more than {@code max} blocks were found. */
    public static List<BlockPos> scan(Level level, BlockPos start, int max) {
        List<BlockPos> result = new ArrayList<>();
        if (level.getBlockState(start).isAir()) return result;

        boolean ignoreTerrain = Config.IGNORE_TERRAIN.get();
        int[][] offsets = Config.DIAGONAL.get() ? ALL : FACE;

        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos s = start.immutable();
        visited.add(s.asLong());
        result.add(s);
        queue.add(s);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (int[] o : offsets) {
                BlockPos n = cur.offset(o[0], o[1], o[2]);
                if (!visited.add(n.asLong())) continue;
                if (!level.isInWorldBounds(n) || !level.isLoaded(n)) continue;
                BlockState st = level.getBlockState(n);
                if (st.isAir()) continue;
                if (ignoreTerrain && isTerrain(st)) continue;
                if (Config.isExtraIgnored(st)) continue;
                if (result.size() >= max) return null;
                result.add(n);
                queue.add(n);
            }
        }
        return result;
    }

    private static boolean isTerrain(BlockState s) {
        return s.is(BlockTags.DIRT)
                || s.is(BlockTags.SAND)
                || s.is(BlockTags.LEAVES)
                || s.is(BlockTags.REPLACEABLE)
                || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.SAPLINGS)
                || s.getBlock() instanceof LiquidBlock
                || s.is(Blocks.STONE)
                || s.is(Blocks.DEEPSLATE)
                || s.is(Blocks.NETHERRACK)
                || s.is(Blocks.END_STONE)
                || s.is(Blocks.GRAVEL)
                || s.is(Blocks.CLAY)
                || s.is(Blocks.BEDROCK);
    }

    private StructureScanner() {}
}
