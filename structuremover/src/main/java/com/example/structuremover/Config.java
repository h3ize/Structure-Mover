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
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_BLOCKS;
    public static final ModConfigSpec.BooleanValue OVERWRITE;
    public static final ModConfigSpec.BooleanValue CREATIVE_ONLY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MAX_BLOCKS = b.comment("Maximum number of blocks a single selection may contain.")
                .defineInRange("maxBlocks", 20000, 1, 500000);
        DIAGONAL = b.comment("If true, blocks touching only by an edge or corner count as connected (26 neighbours). If false, only face-adjacent blocks (6 neighbours).")
                .define("diagonalConnections", true);
        IGNORE_TERRAIN = b.comment("If true, natural blocks (dirt, grass, sand, gravel, stone, deepslate, netherrack, end stone, leaves, plants, liquids) are NOT followed, so the scan does not swallow the whole world.")
                .define("ignoreTerrain", true);
        IGNORED_BLOCKS = b.comment("Extra blocks the scan must not follow. Block ids like \"minecraft:oak_log\" or tags like \"#minecraft:logs\".")
                .defineListAllowEmpty("ignoredBlocks", List.of(), () -> "minecraft:oak_log", o -> o instanceof String);
        OVERWRITE = b.comment("If true, the moved building may replace existing blocks at the destination. If false the move is refused when something is in the way.")
                .define("overwriteExisting", false);
        CREATIVE_ONLY = b.comment("If true, only players in creative mode can use the wand.")
                .define("requireCreativeMode", false);
        SPEC = b.build();
    }

    public static boolean isExtraIgnored(BlockState state) {
        for (String s : IGNORED_BLOCKS.get()) {
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
