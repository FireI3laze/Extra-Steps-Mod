package com.fireblaze.extra_steps.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NoEnchantItem extends Item {

    public NoEnchantItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}