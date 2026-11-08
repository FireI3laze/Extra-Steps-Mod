package com.fireblaze.extra_steps.cauldron.interaction;

import com.fireblaze.extra_steps.blockentity.LyeWaterCauldronBlockEntity;
import com.fireblaze.extra_steps.cauldron.data.CauldronMixingData;
import com.fireblaze.extra_steps.client.color.GenericColors;
import com.fireblaze.extra_steps.item.ModLeatherArmorItem;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import com.fireblaze.extra_steps.util.CauldronInteractionHelper;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ModCauldronInteractions {

    public static final Map<Item, CauldronInteraction> LYE_WATER =
            CauldronInteraction.newInteractionMap();



    // WASHING/DYEING ITEM -> LYE WATER CAULDRON
    public static InteractionResult handleCleaning(
            BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, ItemStack stack
    ) {
        int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        if (currentLevel == 0) return InteractionResult.PASS;

        if (!level.isClientSide) {

            List<ProcessingRecipe> recipes = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.PROCESSING.get());

            ProcessingRecipe recipe = recipes.stream()
                    .filter(r -> r.matches(new SimpleContainer(stack), level))
                    .filter(r -> r.getMode() == ProcessingMode.CLEANING)
                    .findFirst()
                    .orElse(null);

            if (recipe == null) return InteractionResult.sidedSuccess(false);

            if (recipe.getMode() != ProcessingMode.CLEANING) {
                return InteractionResult.PASS;
            }

            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();

            if (level.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be) {

                // Filter out not colorable items todo right now hardcoded
                if (be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) != 0.0f && stack.is(ModItems.SCRAPED_HIDE.get())) return InteractionResult.sidedSuccess(false);

                // Leather Armor Handling
                if (result.is(stack.getItem()) && (result.getItem() instanceof DyeableLeatherItem)) {
                    CauldronInteractionHelper.applyLyeColor(be, stack);
                    if (be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) == 0.0f) ModLeatherArmorItem.clearArmorColor(stack);
                    else ModLeatherArmorItem.setArmorColor(stack, be.getColor());
                }

                // Wool Handling
                else {
                    ItemStack singleItem = stack.split(1);
                    boolean cauldronContainsColor = be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) != 0.0f;
                    if (!cauldronContainsColor) GenericColorHelper.copyColor(stack, result);
                    else CauldronInteractionHelper.applyLyeColor(be, result);
                    CauldronInteractionHelper.replaceItemForPlayer(player, singleItem, result);
                }

                CauldronInteractionHelper.drainLevel(
                        level, pos, state, currentLevel, 1, be,
                        Blocks.CAULDRON.defaultBlockState()
                );

                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH,
                        SoundSource.BLOCKS, 0.5f, 1.0f);

                return InteractionResult.sidedSuccess(false);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }



    // WATER CAULDRON -> LYE WATER CAULDRON
    public static InteractionResult handleLyeWaterCreation(
            BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, ItemStack stack
    ) {
        int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        if (currentLevel == 0) return InteractionResult.PASS;

        if (!level.isClientSide) {
            // Verhindern wenn Mixing-Items im Cauldron liegen
            CauldronMixingData data = CauldronMixingData.get(level);

            if (data != null) {
                NonNullList<ItemStack> inv = data.getInventory(pos);

                boolean hasItems = inv.stream().anyMatch(stackInCauldron -> !stackInCauldron.isEmpty());

                if (hasItems) {
                    return InteractionResult.PASS;
                }
            }

            // Alle Processing-Rezepte durchsuchen
            List<ProcessingRecipe> recipes = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.PROCESSING.get());

            ProcessingRecipe lyeRecipe = recipes.stream()
                    .filter(r -> r.matches(new SimpleContainer(stack), level))
                    .filter(r -> r.getMode() == ProcessingMode.LYE_WATER)
                    .findFirst()
                    .orElse(null);

            // Kein gültiges LYE_WATER-Rezept für diesen ItemStack → Abbruch
            if (lyeRecipe == null) return InteractionResult.PASS;

            // Nur 1 Item verbrauchen
            stack.shrink(1);

            // Neuen Lye Water Cauldron BlockState setzen, Füllstand übernehmen
            BlockState newState = ModBlocks.LYE_WATER_CAULDRON.get()
                    .defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, currentLevel);

            CompoundTag beTag = new CompoundTag();
            if (stack.hasTag()) {
                beTag = stack.getTag().copy();
            }

            CauldronInteractionHelper.setAndReloadBlock(level, pos, newState, beTag);

            // Sound abspielen
            CauldronInteractionHelper.playSound(level, pos, SoundEvents.BREWING_STAND_BREW);

            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.sidedSuccess(true);
    }

    public static void init() {

        // LYE WATER BUCKET -> EMPTY CAULDRON
        CauldronInteraction.EMPTY.put(ModItems.LYE_WATER_BUCKET.get(), (state, level, pos, player, hand, stack) -> {

            if (!level.isClientSide) {

                CompoundTag beTag = new CompoundTag();

                if (stack.hasTag()) {
                    CompoundTag itemTag = stack.getTag();
                    for (String key : itemTag.getAllKeys()) {
                        beTag.put(key, itemTag.get(key).copy());
                    }
                }

                BlockState newState = ModBlocks.LYE_WATER_CAULDRON.get()
                        .defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 3);

                CauldronInteractionHelper.setAndReloadBlock(level, pos, newState, beTag);

                CauldronInteractionHelper.replaceHeldItem(player, stack, new ItemStack(Items.BUCKET));

                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_EMPTY);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // EMPTY BUCKET -> LYE WATER CAULDRON
        LYE_WATER.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {

            if (state.getValue(LayeredCauldronBlock.LEVEL) != 3) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {

                ItemStack result = ModItems.LYE_WATER_BUCKET.get().getDefaultInstance();

                if (level.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be) {
                    CauldronInteractionHelper.applyLyeColor(be, result);
                }

                player.setItemInHand(hand, result);
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            }

            CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_FILL);

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // EMPTY BOTTLE -> LYE WATER CAULDRON
        LYE_WATER.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            if (currentLevel == 0) return InteractionResult.PASS;

            if (!level.isClientSide) {

                ItemStack result = ModItems.LYE_WATER_GLASS_BOTTLE.get().getDefaultInstance();

                if (level.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be) {

                    CauldronInteractionHelper.applyLyeColor(be, result);

                    CauldronInteractionHelper.replaceItemForPlayer(player, stack, result);

                    if (currentLevel == 1) {
                        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                    } else {
                        CauldronInteractionHelper.drainLevel(
                                level,
                                pos,
                                state,
                                currentLevel,
                                3,
                                be,
                                Blocks.CAULDRON.defaultBlockState()
                        );
                    }
                    CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_FILL);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // DYE BOTTLE -> LYE WATER CAULDRON
        LYE_WATER.put(ModItems.LYE_WATER_GLASS_BOTTLE.get(), (state, level, pos, player, hand, stack) -> {
            if (!(level.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be)) return InteractionResult.PASS;

            int bottleFill = stack.getOrCreateTag().getInt(GenericColorHelper.AVAILABLE_INTERACTIONS); // 1-3
            int bottleColor = GenericColorHelper.getColorSmart(stack);

            // Check if cauldron is already max filled
            boolean full = true;
            for (int i = 0; i < 3; i++) {
                if (be.availableInteractions[i] < 3) {
                    full = false;
                    break;
                }
            }
            if (full) return InteractionResult.PASS;

            // Cancle special case: lye water (not dyed) and lye water bottle (dyed)
            boolean cauldronEmptyColor = be.getUsedSlots() == 0; // noch keine Farbe drin
            boolean bottleDyed = stack.hasTag() && stack.getTag().contains(GenericColorHelper.COLOR_TAG);
            if (cauldronEmptyColor && bottleDyed) return InteractionResult.PASS;

            // Same color check
            if (be.getUsedSlots() > 0 && be.getColor() != bottleColor) return InteractionResult.PASS;

            // Fill Cauldron with Bottle
            int remaining = bottleFill;
            for (int i = 0; i < 3 && remaining > 0; i++) {
                int space = 3 - be.availableInteractions[i];
                if (space > 0) {
                    int toAdd = Math.min(remaining, space);
                    be.availableInteractions[i] += toAdd;
                    remaining -= toAdd;
                }
            }

            int newLevel = 0;
            for (int i = 0; i < 3; i++) if (be.availableInteractions[i] > 0) newLevel = i + 1;

            level.setBlockAndUpdate(pos,
                    ModBlocks.LYE_WATER_CAULDRON.get()
                            .defaultBlockState()
                            .setValue(LayeredCauldronBlock.LEVEL, newLevel)
            );

            // Edit Bottle Fill Level
            if (remaining <= 0) {
                ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                CauldronInteractionHelper.replaceItemForPlayer(player, stack, emptyBottle);
            } else {
                ItemStack result = stack.copy().split(1);
                result.getOrCreateTag().putInt(GenericColorHelper.AVAILABLE_INTERACTIONS, remaining);
                CauldronInteractionHelper.replaceItemForPlayer(player, stack, result);
            }

            CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_EMPTY);

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        //DYE BOTTLE -> EMPTY CAULDRON
        CauldronInteraction.EMPTY.put(ModItems.LYE_WATER_GLASS_BOTTLE.get(), (state, level, pos, player, hand, stack) -> {
            if (stack.isEmpty()) return InteractionResult.PASS;

            if (!level.isClientSide) {
                int bottleFill = stack.getOrCreateTag().getInt(GenericColorHelper.AVAILABLE_INTERACTIONS);
                if (bottleFill <= 0) return InteractionResult.PASS;

                // Replace Vanilla Cauldron with Lye Water Cauldron
                BlockState newState = ModBlocks.LYE_WATER_CAULDRON.get()
                        .defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 1);

                level.setBlockAndUpdate(pos, newState);

                // Get fresh created Lye Water Cauldron
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof LyeWaterCauldronBlockEntity lyeBe) {

                    CompoundTag bottleTag = stack.getOrCreateTag();
                    lyeBe.load(bottleTag);

                    // Set new layer and custom fill level
                    Arrays.fill(lyeBe.availableInteractions, 0);
                    lyeBe.availableInteractions[0] = Math.min(bottleFill, 3);

                    lyeBe.setChanged();
                    lyeBe.sync();
                }

                // Empty Bottle
                ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                CauldronInteractionHelper.replaceItemForPlayer(player, stack, emptyBottle);

                // Sound abspielen
                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_EMPTY);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });

        // COLORS -> LYE WATER CAULDRON
        for (var entry : CauldronInteractionHelper.DYE_MAP.entrySet()) {
            GenericColors color = entry.getKey();
            Item dyeItem = entry.getValue();

            LYE_WATER.put(dyeItem, (state, world, pos, player, hand, stack) -> {
                if (world.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be) {
                    if (be.getFull()) return InteractionResult.CONSUME;
                    int level = state.getValue(LayeredCauldronBlock.LEVEL);

                    world.setBlockAndUpdate(pos,
                            ModBlocks.LYE_WATER_CAULDRON.get()
                                    .defaultBlockState()
                                    .setValue(LayeredCauldronBlock.LEVEL, level)
                    );

                    if(!be.mixColor(color.getGenericColor(), level)) return InteractionResult.CONSUME;
                    world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);
                    stack.shrink(1);
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
                return InteractionResult.PASS;
            });
        }



        // WOOL BRUSH -> LYE WATER CAULDRON
        LYE_WATER.put(ModItems.WOOL_BRUSH.get(), (state, level, pos, player, hand, stack) -> {
            if (level.getBlockEntity(pos) instanceof LyeWaterCauldronBlockEntity be) {
                int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                CauldronInteractionHelper.drainLevel(
                        level,
                        pos,
                        state,
                        currentLevel,
                        3,
                        be,
                        Blocks.CAULDRON.defaultBlockState()
                );
                be.setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        });

        BuiltInRegistries.ITEM.forEach(item -> {
            CauldronInteraction original = CauldronInteraction.WATER.get(item);

            CauldronInteraction.WATER.put(item, (state, level, pos, player, hand, stack) -> {

                // 1. Vanilla
                if (original != null) {
                    InteractionResult vanillaResult = original.interact(state, level, pos, player, hand, stack);
                    if (vanillaResult.consumesAction()) return vanillaResult;
                }

                // 2. Lye Water Creation
                InteractionResult lyeWaterResult = ModCauldronInteractions.handleLyeWaterCreation(
                        state, level, pos, player, hand, stack
                );
                if (lyeWaterResult.consumesAction()) return lyeWaterResult;

                // 3. Mixing
                return CauldronMixingHandler.handleMixing(
                        state, level, pos, player, hand, stack
                );
            });
        });

        BuiltInRegistries.ITEM.forEach(item -> {
            CauldronInteraction originalEmpty = CauldronInteraction.EMPTY.get(item);

            CauldronInteraction.EMPTY.put(item, (state, level, pos, player, hand, stack) -> {

                if (hand == InteractionHand.OFF_HAND) {
                    return InteractionResult.PASS;
                }

                // 1. Vanilla
                if (originalEmpty != null) {
                    InteractionResult vanillaResult = originalEmpty.interact(state, level, pos, player, hand, stack);
                    if (vanillaResult.consumesAction()) return vanillaResult;
                }

                // 2. Mixing
                return CauldronMixingHandler.handleMixing(
                        state, level, pos, player, hand, stack
                );
            });
        });
    }
}
