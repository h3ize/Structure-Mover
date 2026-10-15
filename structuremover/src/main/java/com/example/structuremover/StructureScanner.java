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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

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

    public record ScanResult(List<BlockPos> positions, int terrainTouched, int leaves, boolean tooBig) {}
    private record Node(BlockPos pos, int dist) {}

    /** If the player clicked a leaf, walk down the leaf-distance gradient to the log that holds it up. */
    public static BlockPos resolveStart(Level level, BlockPos clicked) {
        BlockState first = level.getBlockState(clicked);
        if (!first.is(BlockTags.LEAVES) || !first.hasProperty(LeavesBlock.DISTANCE)) return clicked;
        BlockPos cur = clicked;
        for (int guard = 0; guard < 8; guard++) {
            int curDist = level.getBlockState(cur).getValue(LeavesBlock.DISTANCE);
            BlockPos next = null;
            for (int[] o : FACE) {
                BlockPos n = cur.offset(o[0], o[1], o[2]);
                BlockState ns = level.getBlockState(n);
                if (ns.is(BlockTags.LOGS)) return n;
                if (next == null && ns.is(BlockTags.LEAVES) && ns.hasProperty(LeavesBlock.DISTANCE)
                        && ns.getValue(LeavesBlock.DISTANCE) < curDist) {
                    next = n;
                }
            }
            if (next == null) return clicked;
            cur = next;
        }
        return clicked;
    }

    public static ScanResult scan(Level level, BlockPos start, int max) {
        List<BlockPos> result = new ArrayList<>();
        if (level.getBlockState(start).isAir()) return new ScanResult(result, 0, 0, false);

        boolean ignoreTerrain = Config.IGNORE_TERRAIN.get();
        int[][] offsets = Config.DIAGONAL.get() ? ALL : FACE;

        Set<Long> visited = new HashSet<>();
        Set<Long> selected = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos s = start.immutable();
        visited.add(s.asLong());
        selected.add(s.asLong());
        result.add(s);
        queue.add(s);

        int terrain = 0;
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (int[] o : offsets) {
                BlockPos n = cur.offset(o[0], o[1], o[2]);
                if (!visited.add(n.asLong())) continue;
                if (!level.isInWorldBounds(n) || !level.isLoaded(n)) continue;
                BlockState st = level.getBlockState(n);
                if (st.isAir()) continue;
                if (ignoreTerrain && isTerrain(st) && !Config.isForceIncluded(st)) {
                    if (isGroundMaterial(st)) terrain++;
                    continue;
                }
                if (Config.isExtraIgnored(st)) continue;
                if (result.size() >= max) return new ScanResult(result, terrain, 0, true);
                BlockPos ni = n.immutable();
                result.add(ni);
                selected.add(ni.asLong());
                queue.add(ni);
            }
        }

        // Trees: pull in the leaves that belong to any selected log.
        int leaves = 0;
        if (Config.TREE_LEAVES.get()) {
            ArrayDeque<Node> q = new ArrayDeque<>();
            for (BlockPos p : result) {
                if (level.getBlockState(p).is(BlockTags.LOGS)) q.add(new Node(p, 0));
            }
            while (!q.isEmpty()) {
                Node cur = q.poll();
                for (int[] o : FACE) {
                    BlockPos n = cur.pos().offset(o[0], o[1], o[2]);
                    if (selected.contains(n.asLong())) continue;
                    if (!level.isInWorldBounds(n) || !level.isLoaded(n)) continue;
                    BlockState st = level.getBlockState(n);
                    if (st.is(BlockTags.LEAVES) && st.hasProperty(LeavesBlock.DISTANCE)
                            && st.getValue(LeavesBlock.DISTANCE) == cur.dist() + 1) {
                        if (result.size() >= max) return new ScanResult(result, terrain, leaves, true);
                        BlockPos ni = n.immutable();
                        selected.add(ni.asLong());
                        result.add(ni);
                        leaves++;
                        q.add(new Node(ni, cur.dist() + 1));
                    }
                }
            }
        }
        return new ScanResult(result, terrain, leaves, false);
    }

    /** Solid natural material that makes up the ground. */
    public static boolean isGroundMaterial(BlockState s) {
        return s.is(BlockTags.DIRT)
                || s.is(BlockTags.SAND)
                || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.BASE_STONE_NETHER)
                || s.is(BlockTags.NYLIUM)
                || s.is(BlockTags.ICE)
                || s.is(BlockTags.SNOW)
                || s.is(Tags.Blocks.ORES)
                || s.is(Blocks.GRAVEL)
                || s.is(Blocks.CLAY)
                || s.is(Blocks.BEDROCK)
                || s.is(Blocks.END_STONE)
                || s.is(Blocks.SOUL_SAND)
                || s.is(Blocks.SOUL_SOIL)
                || s.is(Blocks.MAGMA_BLOCK)
                || s.is(Blocks.DRIPSTONE_BLOCK)
                || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.CALCITE)
                || s.is(Blocks.SMOOTH_BASALT);
    }

    /** Loose natural cover: grass, flowers, saplings, snow layers. Vines and lichen are NOT included so wall decoration moves with the building. */
    public static boolean isGroundCover(BlockState s) {
        if (s.is(Blocks.VINE) || s.is(Blocks.GLOW_LICHEN)) return false;
        return s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS);
    }

    /** Anything the automatic terrain detection treats as nature rather than building. */
    public static boolean isTerrain(BlockState s) {
        return s.getBlock() instanceof LiquidBlock
                || s.is(BlockTags.LEAVES)
                || isGroundCover(s)
                || isGroundMaterial(s);
    }

    private StructureScanner() {}
}
