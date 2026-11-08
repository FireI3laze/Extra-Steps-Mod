package com.fireblaze.extra_steps.compat;

import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class JEIHelper {
    public static ItemStack getIconForMode(ProcessingMode mode) {
        return switch (mode) {
            case DRYING, SCRAPING, BRUSHING -> new ItemStack(ModBlocks.DRYING_RACK.get());
            case COMPRESSING -> new ItemStack(ModBlocks.BASKET.get());
            case CLEANING, MIXING, LYE_WATER -> new ItemStack(ModBlocks.WOODEN_CAULDRON.get());
        };
    }
}
