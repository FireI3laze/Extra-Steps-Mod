package com.fireblaze.extra_steps.client.color;

import com.fireblaze.extra_steps.registry.ModItems;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class GenericItemColoring implements ItemColor {

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFF;

        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains("color")) {
            return tag.getInt("color");
        } else if (stack.is(ModItems.LYE_WATER_GLASS_BOTTLE.get())) {
            return 0xAAC4C4CC;
        }

        return 0xFFFFFF;
    }
}