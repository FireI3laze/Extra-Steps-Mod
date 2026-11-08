package com.fireblaze.extra_steps.util;

import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import com.fireblaze.extra_steps.registry.ModTags;

import java.util.Optional;

public class ScrapingHelper {

    public static boolean canScrape(ItemStack target, Player player, Optional<ProcessingRecipe> recipe) {

        boolean matchesIngredient = recipe.get().getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(target));

        if (matchesIngredient) {
            ItemStack held = player.getMainHandItem();

            return recipe.map(processingRecipe -> processingRecipe.isToolValid(held)).orElse(false);
        }

        return false;
    }
}