package com.fireblaze.extra_steps.util;

import com.fireblaze.extra_steps.blockentity.DryingRackBlockEntity;
import com.fireblaze.extra_steps.processing.ByproductTrigger;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class InteractionCounter {

    public static class State {
        public int hits = 0;
        public int cooldown = 0;
    }

    /**
     * Generic interaction handler (BlockEntities like Brushing / Scraping / Mixing-like BE systems)
     */
    public static boolean handle(
            State state,
            Player player,
            ItemStack stack,
            ProcessingRecipe recipe,
            int cooldownTicks,
            Consumer<Player> onHit,
            Runnable onFinish
    ) {

        if (stack.isEmpty()) return false;

        if (state.cooldown > 0) {
            return false;
        }

        state.cooldown = cooldownTicks;

        float progress = getProgress(stack);
        float step = 1f / recipe.getInteractionCount();

        progress += step;

        setProgress(stack, progress);

        onHit.accept(player);

        // fertig?
        if (progress >= 1f) {

            setProgress(stack, 0f);

            onFinish.run();

            return true;
        }

        return false;
    }

    private static float getProgress(ItemStack stack) {

        if (!stack.hasTag())
            return 0f;

        return stack.getTag().getFloat("processingProgress");
    }

    private static void setProgress(ItemStack stack, float value) {

        if (value <= 0f) {

            if (stack.hasTag()) {

                var tag = stack.getTag();

                if (tag != null) {

                    tag.remove("processingProgress");

                    if (tag.isEmpty()) {
                        stack.setTag(null);
                    }
                }
            }

            return;
        }

        stack.getOrCreateTag().putFloat("processingProgress", value);
    }
}