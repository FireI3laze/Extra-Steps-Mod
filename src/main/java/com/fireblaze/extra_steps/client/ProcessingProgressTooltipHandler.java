package com.fireblaze.extra_steps.client;

import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ProcessingProgressTooltipHandler {
    static int color;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!stack.hasTag())
            return;

        if (!stack.getTag().contains("processingProgress"))
            return;

        if (event.getEntity() == null)
            return;

        float progress =
                stack.getTag().getFloat("processingProgress");

        Optional<ProcessingRecipe> recipe =
                event.getEntity()
                        .level()
                        .getRecipeManager()
                        .getAllRecipesFor(ModRecipeTypes.PROCESSING.get())
                        .stream()
                        .filter(r ->
                                (r.getMode() == ProcessingMode.BRUSHING
                                        || r.getMode() == ProcessingMode.SCRAPING)
                                        &&
                                        r.matches(
                                                new SimpleContainer(stack),
                                                event.getEntity().level()
                                        )
                        )
                        .findFirst();

        if (recipe.isEmpty())
            return;

        int total = recipe.get().getInteractionCount();

        int current = Math.max(
                0,
                Math.min(total,
                        (int)Math.floor(progress * total))
        );

        String action =
                recipe.get().getMode() == ProcessingMode.BRUSHING
                        ? "brushes"
                        : "scrapes";

        event.getToolTip().add(
                Component.literal(
                        current + "/" + total + " " + action
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}