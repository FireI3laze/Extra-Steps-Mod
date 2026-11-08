package com.fireblaze.extra_steps.cauldron.interaction;

import com.fireblaze.extra_steps.blockentity.WoodenLyeWaterCauldronBlockEntity;
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
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class WoodenCauldronInteractions {

    public static final Map<Item, CauldronInteraction> WOODEN_EMPTY = CauldronInteraction.newInteractionMap();
    public static final Map<Item, CauldronInteraction> WOODEN_WATER = CauldronInteraction.newInteractionMap();
    public static final Map<Item, CauldronInteraction> WOODEN_LYE_WATER = CauldronInteraction.newInteractionMap();



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

            if (level.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be) {

                // Filter out not colorable items todo right now hardcoded
                if (be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) != 0.0f && stack.is(ModItems.SCRAPED_HIDE.get())) return InteractionResult.sidedSuccess(false);

                if (result.is(stack.getItem()) && (result.getItem() instanceof DyeableLeatherItem)) {
                    CauldronInteractionHelper.applyLyeColor(be, stack);
                    if (be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) == 0.0f) ModLeatherArmorItem.clearArmorColor(stack);
                    else ModLeatherArmorItem.setArmorColor(stack, be.getColor());

                } else {
                    ItemStack singleItem = stack.split(1);
                    // Get correct NBT & Color
                    boolean cauldronContainsColor = be.getUpdateTag().getFloat(GenericColorHelper.FILL_FACTOR) != 0.0f;
                    if (!cauldronContainsColor) GenericColorHelper.copyColor(stack, result);
                    else CauldronInteractionHelper.applyLyeColor(be, result);
                    CauldronInteractionHelper.replaceItemForPlayer(player, singleItem, result);
                }

                CauldronInteractionHelper.drainLevel(
                        level, pos, state, currentLevel, 1, be,
                        ModBlocks.WOODEN_CAULDRON.get().defaultBlockState()
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
            BlockState newState = ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
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

        // WATER BUCKET -> EMPTY CAULDRON
        WOODEN_EMPTY.put(Items.WATER_BUCKET, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                level.setBlock(pos,
                        ModBlocks.WOODEN_WATER_CAULDRON.get()
                                .defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 3),
                        3
                );

                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_FILL);

                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });

        // EMPTY BUCKET -> WATER CAULDRON
        WOODEN_WATER.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
                if (!level.isClientSide) {

                    player.setItemInHand(hand,
                            ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));

                    level.setBlock(pos, ModBlocks.WOODEN_CAULDRON.get().defaultBlockState(), 3);

                    CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_EMPTY);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        });

        // WATER BOTTLE -> EMPTY CAULDRON
        WOODEN_EMPTY.put(Items.POTION, (state, level, pos, player, hand, stack) -> {
            if (PotionUtils.getPotion(stack) != Potions.WATER) return InteractionResult.PASS;

            if (!level.isClientSide) {
                level.setBlock(pos,
                        ModBlocks.WOODEN_WATER_CAULDRON.get()
                                .defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 1),
                        3
                );

                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_EMPTY);

                CauldronInteractionHelper.replaceItemForPlayer(player, stack, Items.GLASS_BOTTLE.getDefaultInstance());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });

        // WATER BOTTLE -> WATER CAULDRON
        WOODEN_WATER.put(Items.POTION, (state, level, pos, player, hand, stack) -> {
            if (PotionUtils.getPotion(stack) != Potions.WATER) return InteractionResult.PASS;

            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);

            if (currentLevel >= 3) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                level.setBlock(pos,
                        state.setValue(LayeredCauldronBlock.LEVEL, currentLevel + 1),
                        3
                );

                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_EMPTY);

                CauldronInteractionHelper.replaceItemForPlayer(
                        player,
                        stack,
                        Items.GLASS_BOTTLE.getDefaultInstance()
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });

        // EMPTY GLASS BOTTLE -> WATER CAULDRON
        WOODEN_WATER.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {

                ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, potion));

                int levelValue = state.getValue(LayeredCauldronBlock.LEVEL);

                if (levelValue > 1) {
                    level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, levelValue - 1), 3);
                } else {
                    level.setBlock(pos, ModBlocks.WOODEN_CAULDRON.get().defaultBlockState(), 3);
                }
                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_FILL);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // LYE WATER BUCKET -> EMPTY CAULDRON
        WOODEN_EMPTY.put(ModItems.LYE_WATER_BUCKET.get(), (state, level, pos, player, hand, stack) -> {

            if (!level.isClientSide) {

                CompoundTag beTag = new CompoundTag();

                if (stack.hasTag()) {
                    CompoundTag itemTag = stack.getTag();
                    for (String key : itemTag.getAllKeys()) {
                        beTag.put(key, itemTag.get(key).copy());
                    }
                }

                BlockState newState = ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
                        .defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 3);

                CauldronInteractionHelper.setAndReloadBlock(level, pos, newState, beTag);

                CauldronInteractionHelper.replaceHeldItem(player, stack, new ItemStack(Items.BUCKET));

                CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_EMPTY);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // EMPTY BUCKET -> LYE WATER CAULDRON
        WOODEN_LYE_WATER.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {

            if (state.getValue(LayeredCauldronBlock.LEVEL) != 3) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {

                ItemStack result = ModItems.LYE_WATER_BUCKET.get().getDefaultInstance();

                if (level.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be) {
                    CauldronInteractionHelper.applyLyeColor(be, result);
                }

                player.setItemInHand(hand, result);
                level.setBlockAndUpdate(pos, ModBlocks.WOODEN_CAULDRON.get().defaultBlockState());
            }

            CauldronInteractionHelper.playSound(level, pos, SoundEvents.BUCKET_FILL);

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // EMPTY BOTTLE -> LYE WATER CAULDRON
        WOODEN_LYE_WATER.put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            if (currentLevel == 0) return InteractionResult.PASS;

            if (!level.isClientSide) {

                ItemStack result = ModItems.LYE_WATER_GLASS_BOTTLE.get().getDefaultInstance();

                if (level.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be) {

                    CauldronInteractionHelper.applyLyeColor(be, result);

                    CauldronInteractionHelper.replaceItemForPlayer(player, stack, result);

                    if (currentLevel == 1) {
                        level.setBlockAndUpdate(pos, ModBlocks.WOODEN_CAULDRON.get().defaultBlockState());
                    } else {
                        CauldronInteractionHelper.drainLevel(
                                level,
                                pos,
                                state,
                                currentLevel,
                                3,
                                be,
                                ModBlocks.WOODEN_CAULDRON.get().defaultBlockState()
                        );
                    }
                    CauldronInteractionHelper.playSound(level, pos, SoundEvents.BOTTLE_FILL);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });



        // DYE BOTTLE -> LYE WATER CAULDRON
        WOODEN_LYE_WATER.put(ModItems.LYE_WATER_GLASS_BOTTLE.get(), (state, level, pos, player, hand, stack) -> {
            if (!(level.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be)) return InteractionResult.PASS;

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
            /* todo needed?
            be.setChanged();
            be.sync();
            */

            int newLevel = 0;
            for (int i = 0; i < 3; i++) if (be.availableInteractions[i] > 0) newLevel = i + 1;

            level.setBlockAndUpdate(pos,
                    ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
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
        WOODEN_EMPTY.put(ModItems.LYE_WATER_GLASS_BOTTLE.get(), (state, level, pos, player, hand, stack) -> {
            if (stack.isEmpty()) return InteractionResult.PASS;

            if (!level.isClientSide) {
                int bottleFill = stack.getOrCreateTag().getInt(GenericColorHelper.AVAILABLE_INTERACTIONS);
                if (bottleFill <= 0) return InteractionResult.PASS;

                // Replace Vanilla Cauldron with Lye Water Cauldron
                BlockState newState = ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
                        .defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 1);

                level.setBlockAndUpdate(pos, newState);

                // Get fresh created Lye Water Cauldron
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof WoodenLyeWaterCauldronBlockEntity lyeBe) {

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

            WOODEN_LYE_WATER.put(dyeItem, (state, world, pos, player, hand, stack) -> {
                if (world.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be) {
                    if (be.getFull()) return InteractionResult.CONSUME;
                    int level = state.getValue(LayeredCauldronBlock.LEVEL);

                    world.setBlockAndUpdate(pos,
                            ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
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
        WOODEN_LYE_WATER.put(ModItems.WOOL_BRUSH.get(), (state, level, pos, player, hand, stack) -> {
            if (level.getBlockEntity(pos) instanceof WoodenLyeWaterCauldronBlockEntity be) {
                int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                CauldronInteractionHelper.drainLevel(
                        level,
                        pos,
                        state,
                        currentLevel,
                        3,
                        be,
                        ModBlocks.WOODEN_CAULDRON.get().defaultBlockState()
                );
                be.setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        });

        BuiltInRegistries.ITEM.forEach(item -> {
            if (!WOODEN_LYE_WATER.containsKey(item)) {
                WOODEN_LYE_WATER.put(item, WoodenCauldronInteractions::handleCleaning);
            }
        });

        BuiltInRegistries.ITEM.forEach(item -> {
            CauldronInteraction original = WOODEN_WATER.get(item);

            WOODEN_WATER.put(item, (state, level, pos, player, hand, stack) -> {

                // 1. Vanilla
                if (original != null) {
                    InteractionResult vanillaResult = original.interact(state, level, pos, player, hand, stack);
                    if (vanillaResult.consumesAction()) return vanillaResult;
                }

                // 2. Lye Water Creation
                InteractionResult lyeWaterResult = WoodenCauldronInteractions.handleLyeWaterCreation(
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
            CauldronInteraction originalEmpty = WOODEN_EMPTY.get(item);

            WOODEN_EMPTY.put(item, (state, level, pos, player, hand, stack) -> {

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