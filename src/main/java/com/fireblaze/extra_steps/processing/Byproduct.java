package com.fireblaze.extra_steps.processing;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public record Byproduct(
        ItemStack stack,
        float chance,
        ByproductTrigger trigger
) {

    public boolean roll(RandomSource random) {
        return random.nextFloat() <= chance;
    }
}

