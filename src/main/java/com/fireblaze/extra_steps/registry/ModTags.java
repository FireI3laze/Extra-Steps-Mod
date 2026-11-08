package com.fireblaze.extra_steps.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import com.fireblaze.extra_steps.ExtraSteps;

public class ModTags {

    public static final TagKey<Item> RAW_HIDES =
            TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.fromNamespaceAndPath(ExtraSteps.MODID, "raw_hides"));
    public static class Items {
        public static final TagKey<Item> COLORABLE_BLOCKS =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("extra_steps", "colorable_blocks"));
    }

    public static class Blocks {
        public static final TagKey<Block> VALID_MIXING_CAULDRONS =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("extra_steps", "valid_mixing_cauldrons"));
    }

    public static final TagKey<Item> COLORABLE_BLOCKS = Items.COLORABLE_BLOCKS;

    public static final TagKey<Block> VALID_MIXING_CAULDRONS = Blocks.VALID_MIXING_CAULDRONS;
}