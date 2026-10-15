package com.example.structuremover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MoveService {
    /** Block updates are sent to clients, but no neighbour/shape updates happen while placing, so nothing pops off mid-move. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public record Snap(BlockState state, CompoundTag tag) {}
    public record UndoData(ResourceKey<Level> dimension, Map<BlockPos, Snap> snapshot) {}
    public record Result(boolean ok, String message) {}
    private record Entry(BlockState state, CompoundTag tag, BlockPos newPos) {}

    private static final Map<UUID, UndoData> UNDO = new HashMap<>();

    public static void forget(UUID id) { UNDO.remove(id); }

    public static Result move(ServerPlayer player, ServerLevel level, Selection sel, BlockPos targetAnchor) {
        HolderLookup.Provider reg = level.registryAccess();
        Rotation rot = StructureMath.toRotation(sel.rotation);
        BlockPos anchor = sel.anchor;

        Set<Long> source = new HashSet<>();
        for (BlockPos p : sel.positions) source.add(p.asLong());

        List<Entry> entries = new ArrayList<>();
        Map<BlockPos, Snap> snapshot = new LinkedHashMap<>();

        for (BlockPos p : sel.positions) {
            BlockState st = level.getBlockState(p);
            if (st.isAir()) continue; // changed since selection
            BlockEntity be = level.getBlockEntity(p);
            CompoundTag tag = be != null ? be.saveWithFullMetadata(reg) : null;

            int[] r = StructureMath.rotate(p.getX() - anchor.getX(), p.getZ() - anchor.getZ(), sel.rotation);
            BlockPos np = new BlockPos(
                    targetAnchor.getX() + r[0],
                    targetAnchor.getY() + (p.getY() - anchor.getY()),
                    targetAnchor.getZ() + r[1]);

            if (!level.isInWorldBounds(np)) return new Result(false, "The building would leave the world height limits.");
            if (!level.isLoaded(np)) return new Result(false, "The destination is not fully loaded.");

            entries.add(new Entry(st, tag, np));
            snapshot.put(p.immutable(), new Snap(st, tag));
        }
        if (entries.isEmpty()) return new Result(false, "The selected building no longer exists.");

        int blocked = 0;
        int carved = 0;
        BlockPos firstBlocked = null;
        boolean replaceTerrain = Config.REPLACE_TERRAIN.get();
        for (Entry e : entries) {
            if (source.contains(e.newPos().asLong())) continue;
            BlockState existing = level.getBlockState(e.newPos());
            boolean free = existing.isAir() || existing.canBeReplaced() || existing.getBlock() instanceof LiquidBlock;
            if (!free && replaceTerrain && StructureScanner.isTerrain(existing) && !Config.isForceIncluded(existing)) {
                free = true;
                carved++;
            }
            if (!free && !Config.OVERWRITE.get()) {
                blocked++;
                if (firstBlocked == null) firstBlocked = e.newPos();
            }
            if (!snapshot.containsKey(e.newPos())) {
                BlockEntity be = level.getBlockEntity(e.newPos());
                snapshot.put(e.newPos().immutable(), new Snap(existing, be != null ? be.saveWithFullMetadata(reg) : null));
            }
        }
        if (blocked > 0) {
            return new Result(false, "Blocked by " + blocked + " existing block(s), first at "
                    + firstBlocked.getX() + " " + firstBlocked.getY() + " " + firstBlocked.getZ()
                    + ". Pick a clearer spot or enable overwriteExisting in the config.");
        }

        // Phase 1: clear every affected position (block entities first so containers never drop items).
        for (BlockPos p : snapshot.keySet()) clear(level, p);

        // Phase 2: place the rotated building.
        for (Entry e : entries) {
            level.setBlock(e.newPos(), e.state().rotate(rot), FLAGS);
            if (e.tag() != null) {
                BlockEntity be = level.getBlockEntity(e.newPos());
                if (be != null) {
                    be.loadWithComponents(e.tag().copy(), reg);
                    be.setChanged();
                    BlockState now = level.getBlockState(e.newPos());
                    level.sendBlockUpdated(e.newPos(), now, now, Block.UPDATE_ALL);
                }
            }
        }

        // Phase 3: let neighbours react (redstone, supports, etc).
        notifyAll(level, snapshot.keySet());

        level.playSound(null, targetAnchor, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        UNDO.put(player.getUUID(), new UndoData(level.dimension(), snapshot));
        return new Result(true, "Moved " + entries.size() + " blocks" + (carved > 0 ? " (replaced " + carved + " terrain blocks)" : "") + ". /structuremover undo reverts it.");
    }

    public static Result undo(ServerPlayer player) {
        UndoData data = UNDO.remove(player.getUUID());
        if (data == null) return new Result(false, "Nothing to undo.");
        ServerLevel level = player.server.getLevel(data.dimension());
        if (level == null) return new Result(false, "That dimension is not available.");
        HolderLookup.Provider reg = level.registryAccess();

        for (BlockPos p : data.snapshot().keySet()) clear(level, p);
        for (Map.Entry<BlockPos, Snap> e : data.snapshot().entrySet()) {
            Snap s = e.getValue();
            if (s.state().isAir()) continue;
            level.setBlock(e.getKey(), s.state(), FLAGS);
            if (s.tag() != null) {
                BlockEntity be = level.getBlockEntity(e.getKey());
                if (be != null) {
                    be.loadWithComponents(s.tag().copy(), reg);
                    be.setChanged();
                    BlockState now = level.getBlockState(e.getKey());
                    level.sendBlockUpdated(e.getKey(), now, now, Block.UPDATE_ALL);
                }
            }
        }
        notifyAll(level, data.snapshot().keySet());
        return new Result(true, "Undid the last move (" + data.snapshot().size() + " positions restored).");
    }

    private static void clear(ServerLevel level, BlockPos p) {
        level.removeBlockEntity(p);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
    }

    private static void notifyAll(ServerLevel level, Set<BlockPos> positions) {
        for (BlockPos p : positions) {
            level.updateNeighborsAt(p, level.getBlockState(p).getBlock());
        }
    }

    private MoveService() {}
}
