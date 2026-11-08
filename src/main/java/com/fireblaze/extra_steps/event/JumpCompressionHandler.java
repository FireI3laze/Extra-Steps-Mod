package com.fireblaze.extra_steps.event;

import com.fireblaze.extra_steps.ExtraSteps;
import com.fireblaze.extra_steps.blockentity.BasketBlockEntity;
import com.fireblaze.extra_steps.client.color.GenericColors;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = ExtraSteps.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JumpCompressionHandler {

    @SubscribeEvent
    public static void onPlayerLand(LivingFallEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // Nur echte Landungen (optional Threshold)
        if (event.getDistance() < 1.0f) return;

        // Block unter dem Spieler
        BlockPos pos = player.blockPosition();
        if (!(player.level().getBlockEntity(pos) instanceof BasketBlockEntity basket)) return;

        // 🔥 AABB des Blocks holen
        var shape = basket.getBlockState().getShape(player.level(), pos);
        var box = shape.bounds().move(pos);

        // Prüfen ob Spieler wirklich im Korb ist
        if (!box.contains(player.position())) return;

        // --- Dein bisheriger Code ---
        ItemStack stack = basket.getInventoryHandler().getStackInSlot(0);
        if (stack.isEmpty()) return;

        var recipeOpt = basket.getRecipeByMode(ProcessingMode.COMPRESSING);
        if (recipeOpt.isEmpty()) return;

        var recipe = recipeOpt.get();
        boolean matchesIngredient = recipe.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(stack));
        if (!matchesIngredient) return;

        //if (basket.getInventoryHandler().getStackInSlot(0).getCount() < recipe.getIngredientAmount()) return;
        if (basket.getInventoryHandler().getStackInSlot(0).getCount() < recipe.getCountedIngredients().get(0).count()) return;
        basket.getInventoryHandler().extractItem(0, recipe.getCountedIngredients().get(0).count(), false);

        ItemStack inputCopy = stack.copy();
        ItemStack drop;



        if (stack.is(ModItems.BRUSHED_WOOL.get()) && !(stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains(GenericColorHelper.FILL_FACTOR))) {
            // Vanilla Wool in der richtigen Farbe
            // 1. Ermittle die Farbe vom Input
            int colorRGB = GenericColorHelper.getColorSmart(inputCopy);

            // 2. Finde das nächste GenericColors Enum
            GenericColors closest = GenericColorHelper.getClosestGenericColor(colorRGB);

            // 3. Hole das passende Vanilla Wool Block
            Block vanillaWool = GenericColorHelper.getBlockFromGenericColor(closest);

            // 4. Erstelle das ItemStack zum Droppen
            drop = new ItemStack(
                    vanillaWool.asItem(),
                    recipe.getResultItem(null).getCount()
            );
        } else if (stack.hasTag() && stack.getTag().contains(GenericColorHelper.FILL_FACTOR)) {

            drop = recipe.getResultItem(null).copy();

            // Farbe vom Input übernehmen
            GenericColorHelper.copyColor(inputCopy, drop);

        } else {
            drop = recipe.getResultItem(null).copy();
        }

        player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                drop
        ));

        recipe.getByproducts().forEach(byproduct -> {
            // Chance prüfen
            if (!byproduct.roll(player.level().random))
                return;

            ItemStack byproductStack = byproduct.stack().copy();

            // Optional: Farbe übernehmen
            if (GenericColorHelper.hasColor(inputCopy)) {
                GenericColorHelper.copyColor(inputCopy, byproductStack);
            }

            player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    byproductStack
            ));
        });

        player.level().playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}