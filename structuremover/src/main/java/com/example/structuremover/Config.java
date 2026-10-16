package com.example.structuremover;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_BLOCKS;
    public static final ModConfigSpec.BooleanValue DIAGONAL;
    public static final ModConfigSpec.BooleanValue IGNORE_TERRAIN;
    public static final ModConfigSpec.BooleanValue TREE_LEAVES;
    public static final ModConfigSpec.BooleanValue REPLACE_TERRAIN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_INCLUDE;
    public static final ModConfigSpec.BooleanValue OVERWRITE;
    public static final ModConfigSpec.BooleanValue CREATIVE_ONLY;
    public static final ModConfigSpec.BooleanValue PREVIEW_BLOCKS;
    public static final ModConfigSpec.DoubleValue PREVIEW_OPACITY;
    public static final ModConfigSpec.IntValue PREVIEW_MAX_BLOCKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MAX_BLOCKS = b.comment("Maximum number of blocks a single selection may contain.")
                .defineInRange("maxBlocks", 20000, 1, 500000);
        DIAGONAL = b.comment("If true, blocks touching only by an edge or corner count as connected (26 neighbours). If false, only face-adjacent blocks (6 neighbours).")
                .define("diagonalConnections", true);
        IGNORE_TERRAIN = b.comment("Automatic terrain detection: natural blocks (dirt, grass, sand, gravel, stone types, ores, nylium, ice, snow, plants, liquids...) are not followed, so the scan never swallows the ground.")
                .define("ignoreTerrain", true);
        TREE_LEAVES = b.comment("If a tree (logs) is part of the selection, the leaves that belong to it are moved too.")
                .define("moveTreeLeaves", true);
        REPLACE_TERRAIN = b.comment("If true, natural terrain at the destination (dirt, stone, grass, leaves, plants, liquids...) is replaced by the moved building instead of blocking it. Undo restores it.")
                .define("replaceTerrainAtDestination", true);
        IGNORED_BLOCKS = b.comment("Extra blocks the scan must not follow. Block ids like \"minecraft:oak_log\" or tags like \"#minecraft:logs\".")
                .defineListAllowEmpty("ignoredBlocks", List.of(), () -> "minecraft:oak_log", o -> o instanceof String);
        ALWAYS_INCLUDE = b.comment("Blocks that are never treated as terrain, for example \"minecraft:stone\" if you build with it. Ids or #tags.")
                .defineListAllowEmpty("alwaysIncludeBlocks", List.of(), () -> "minecraft:stone", o -> o instanceof String);
        OVERWRITE = b.comment("If true, the moved building may replace ANY existing block at the destination. If false the move is refused when a non-terrain block is in the way.")
                .define("overwriteExisting", false);
        CREATIVE_ONLY = b.comment("If true, only players in creative mode can use the wand.")
                .define("requireCreativeMode", false);
        PREVIEW_BLOCKS = b.comment("Show a transparent preview of the selected blocks where they will be placed.")
                .define("showBlockPreview", true);
        PREVIEW_OPACITY = b.comment("How see-through the placement preview is (1.0 = solid).")
                .defineInRange("previewOpacity", 0.5, 0.1, 1.0);
        PREVIEW_MAX_BLOCKS = b.comment("Most blocks drawn in the placement preview. Bigger buildings are thinned out evenly to keep the framerate up.")
                .defineInRange("previewMaxBlocks", 2500, 100, 20000);
        SPEC = b.build();
    }

    public static boolean isExtraIgnored(BlockState state) {
        return matches(IGNORED_BLOCKS.get(), state);
    }

    public static boolean isForceIncluded(BlockState state) {
        return matches(ALWAYS_INCLUDE.get(), state);
    }

    private static boolean matches(List<? extends String> list, BlockState state) {
        if (list.isEmpty()) return false;
        for (String s : list) {
            if (s.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(s.substring(1));
                if (id != null && state.is(TagKey.create(Registries.BLOCK, id))) return true;
            } else if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(s)) {
                return true;
            }
        }
        return false;
    }

    private Config() {}
}
