package com.fireblaze.extra_steps.cauldron.interaction;

import com.fireblaze.extra_steps.cauldron.data.CauldronMixingData;
import com.fireblaze.extra_steps.processing.Byproduct;
import com.fireblaze.extra_steps.processing.ByproductTrigger;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import com.fireblaze.extra_steps.registry.ModSounds;
import com.fireblaze.extra_steps.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;

public class CauldronMixingHandler {
    private static final List<RegistryObject<SoundEvent>> STIR_SOUNDS = List.of(
            ModSounds.STIR_1,
            ModSounds.STIR_2,
            ModSounds.STIR_3,
            ModSounds.STIR_4
    );

    private static final List<RegistryObject<SoundEvent>> STIR_FINISH_SOUNDS = List.of(
            ModSounds.STIR_FINISH_1,
            ModSounds.STIR_FINISH_2
    );

    private static SoundEvent randomStir() {
        return STIR_SOUNDS.get(new java.util.Random().nextInt(STIR_SOUNDS.size())).get();
    }

    private static SoundEvent randomFinish() {
        return STIR_FINISH_SOUNDS.get(new java.util.Random().nextInt(STIR_FINISH_SOUNDS.size())).get();
    }

    public static InteractionResult handleMixing(
            BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, ItemStack stack
    ) {
        if (level.isClientSide) return InteractionResult.PASS;

        if (stack.getItem() instanceof BucketItem) return InteractionResult.PASS;

        if (!state.is(ModTags.Blocks.VALID_MIXING_CAULDRONS)) {
            return InteractionResult.PASS;
        }

        CauldronMixingData data = CauldronMixingData.get(level);
        if (data == null) return InteractionResult.PASS;

        NonNullList<ItemStack> inv = data.getInventory(pos);

        if (stack.isEmpty()) {
            // von hinten nach vorne → letzter belegter Slot
            for (int i = inv.size() - 1; i >= 0; i--) {
                ItemStack stored = inv.get(i);

                if (!stored.isEmpty()) {

                    ItemStack extracted = stored.copy();

                    // Slot leeren
                    inv.set(i, ItemStack.EMPTY);
                    data.setInventory(pos, inv, (ServerLevel) level);

                    // 👉 Erst versuchen ins Spielerinventar
                    boolean inserted = player.getInventory().add(extracted);

                    if (!inserted) {
                        // 👉 Wenn kein Platz → auf Boden beim Spieler
                        player.drop(extracted, false);
                    }

                    level.playSound(null, pos,
                            SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                            SoundSource.BLOCKS,
                            0.5f, 1.0f
                    );

                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.SUCCESS; // nichts drin, aber handled
        }
        else {
            // Noch KEIN fertiges Rezept → Ingredient hinzufügen
            // 1. Ingredients immer einlagern
            if (isValidIngredient(level, stack)) {

                for (int i = 0; i < inv.size(); i++) {

                    if (inv.get(i).isEmpty()) {

                        inv.set(i, stack.copy());
                        stack.setCount(0);

                        data.setInventory(pos, inv, (ServerLevel) level);

                        level.playSound(
                                null,
                                pos,
                                SoundEvents.GENERIC_SPLASH,
                                SoundSource.BLOCKS,
                                0.3f,
                                1.35f
                        );

                        return InteractionResult.SUCCESS;
                    }
                }

                return InteractionResult.SUCCESS;
            }

// 2. Rezept suchen
            ProcessingRecipe recipe = findMatchingRecipe(level, inv);

            if (recipe == null) {
                return InteractionResult.PASS;
            }

// 3. Tool prüfen
            if (!recipe.isToolValid(stack)) {
                return InteractionResult.PASS;
            }
        }

        ProcessingRecipe recipe = findMatchingRecipe(level, inv);
        if (recipe == null) return InteractionResult.PASS;

        if (!(recipe.isToolValid(stack))) {
            return InteractionResult.PASS;
        }

        int waterLevel = 0;

        if (state.hasProperty(LayeredCauldronBlock.LEVEL)
                && state.getBlock() instanceof LayeredCauldronBlock) {

            waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);

            if (waterLevel < recipe.getWaterLevelReduction()) {
                return InteractionResult.PASS;
            }
        } else if (recipe.getWaterLevelReduction() > 0) return InteractionResult.PASS;

        int hits = data.getHits(pos);
        long gameTime = level.getGameTime();
        long last = data.getLastStirTick(pos);

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5, (10 * recipe.getInteractionCount()), false, false)); //20

        // z.B. 10 Ticks = 0.5 Sekunden
        if (gameTime - last < 10) {
            return InteractionResult.SUCCESS; // blockiert Spam
        }

        // neuen Timestamp setzen
        data.setLastStirTick(pos, gameTime);


        hits++;
        data.addHit(pos);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

        for (Byproduct byproduct : recipe.getByproducts()) {

            if (byproduct.trigger() != ByproductTrigger.PER_INTERACTION)
                continue;

            if (level.random.nextFloat() > byproduct.chance())
                continue;

            dropItem(level, pos, byproduct.stack().copy());
        }

        if (hits < recipe.getInteractionCount()) {
            level.playSound(
                    null,
                    pos,
                    randomStir(),
                    SoundSource.BLOCKS,
                    0.6f,
                    1.0f
            );
            return InteractionResult.SUCCESS; // noch nicht fertig
        }


        level.playSound(
                null,
                pos,
                randomFinish(),
                SoundSource.BLOCKS,
                0.6f,
                1.0f
        );
        data.resetHits(pos);

        // 👉 Zutaten konsumieren
        if (!consumeIngredients(inv, recipe)) {
            return InteractionResult.PASS;
        }

        data.setInventory(pos, inv, (ServerLevel) level);

        // 👉 Wasser reduzieren
        int newLevel = waterLevel - recipe.getWaterLevelReduction();

        Block empty = EMPTY_CAULDRON_MAP.getOrDefault(
                state.getBlock(),
                Blocks.CAULDRON
        );

        if (newLevel <= 0) {
            level.setBlock(pos, empty.defaultBlockState(), 3);
        } else {
            level.setBlock(pos,
                    state.setValue(LayeredCauldronBlock.LEVEL, newLevel),
                    3
            );
        }

        // 👉 Result droppen
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        dropItem(level, pos, result);

        // 👉 Byproducts
        for (Byproduct byproduct : recipe.getByproducts()) {

            if (byproduct.trigger() != ByproductTrigger.ON_COMPLETE)
                continue;

            if (level.random.nextFloat() > byproduct.chance())
                continue;

            dropItem(level, pos, byproduct.stack().copy());
        }

        return InteractionResult.SUCCESS;
    }

    private static void dropItem(Level level, BlockPos pos, ItemStack stack) {
        Containers.dropItemStack(
                level,
                pos.getX() + 0.5,
                pos.getY() + 1.1,
                pos.getZ() + 0.5,
                stack
        );
    }

    public static boolean isValidIngredient(Level level, ItemStack stack) {
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.PROCESSING.get())
                .stream()
                .filter(r -> r.getMode() == ProcessingMode.MIXING)
                .anyMatch(recipe ->
                        recipe.getIngredients().stream()
                                .anyMatch(ingredient -> ingredient.test(stack))
                );
    }

    private static ProcessingRecipe findMatchingRecipe(Level level, NonNullList<ItemStack> inv) {

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.PROCESSING.get())
                .stream()
                .filter(r -> r.getMode() == ProcessingMode.MIXING)
                .filter(r -> matchesInventory(r, inv))
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesInventory(ProcessingRecipe recipe, NonNullList<ItemStack> inv) {

        for (ProcessingRecipe.CountedIngredient ci : recipe.getCountedIngredients()) {

            int needed = ci.count();
            int found = 0;

            for (ItemStack stack : inv) {

                if (!stack.isEmpty() && ci.ingredient().test(stack)) {
                    found += stack.getCount();

                    if (found >= needed) break;
                }
            }

            if (found < needed) return false;
        }

        return true;
    }

    private static boolean consumeIngredients(NonNullList<ItemStack> inv, ProcessingRecipe recipe) {
        for (ProcessingRecipe.CountedIngredient ci : recipe.getCountedIngredients()) {

            int needed = ci.count();
            int remaining = needed;

            for (int i = 0; i < inv.size(); i++) {

                ItemStack stack = inv.get(i);

                if (!stack.isEmpty() && ci.ingredient().test(stack)) {

                    int take = Math.min(stack.getCount(), remaining);

                    stack.shrink(take);
                    remaining -= take;

                    if (stack.isEmpty()) {
                        inv.set(i, ItemStack.EMPTY);
                    }

                    if (remaining <= 0) break;
                }
            }

            if (remaining > 0) return false; // nicht genug Items vorhanden
        }

        return true;
    }

    private static final Map<Block, Block> EMPTY_CAULDRON_MAP = Map.of(
            Blocks.WATER_CAULDRON, Blocks.CAULDRON,
            ModBlocks.WOODEN_WATER_CAULDRON.get(), ModBlocks.WOODEN_CAULDRON.get()
    );
}
