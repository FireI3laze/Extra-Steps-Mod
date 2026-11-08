package com.fireblaze.extra_steps.util;

import com.fireblaze.extra_steps.blockentity.WoodenLyeWaterCauldronBlockEntity;
import com.fireblaze.extra_steps.client.color.GenericColors;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;


import net.minecraft.world.level.block.Blocks;

public class GenericColorHelper {

    public static final String COLOR_TAG = "color";
    public static final String FILL_FACTOR = "fillFactor";
    public static final String COLOR_LIST = "colorList";
    public static final String COLOR_COUNT = "colorCount";
    public static final String AVAILABLE_INTERACTIONS = "availableInteraction";

    /**
     * Setzt eine Farbe auf ein ItemStack
     */
    public static ItemStack setColor(ItemStack stack, GenericColors color) {
        stack.getOrCreateTag().putInt(COLOR_TAG, color.getGenericColor());
        return stack;
    }

    /**
     * Setzt eine Farbe direkt über RGB
     */
    public static ItemStack setColor(ItemStack stack, int rgb) {
        stack.getOrCreateTag().putInt(COLOR_TAG, rgb);
        return stack;
    }

    /**
     * Liest die Farbe eines ItemStacks
     */
    public static int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(COLOR_TAG)) {
            return tag.getInt(COLOR_TAG);
        }

        return GenericColors.WHITE.getGenericColor();
    }

    /**
     * Prüft ob das Item eine Farbe besitzt
     */
    public static boolean hasColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(COLOR_TAG);
    }

    /**
     * Kopiert die Farbe von einem ItemStack auf ein anderes
     */
    public static ItemStack copyColor(ItemStack source, ItemStack target) {
        CompoundTag sourceTag = source.getTag();
        if (sourceTag != null) {
            CompoundTag targetTag = target.getOrCreateTag();

            // Kopiert die Hauptfarbe
            if (sourceTag.contains(COLOR_TAG)) {
                targetTag.putInt(COLOR_TAG, sourceTag.getInt(COLOR_TAG));
            }

            // Kopiert die fillFactor (Saturation Ratio)
            if (sourceTag.contains(FILL_FACTOR)) {
                targetTag.putFloat(FILL_FACTOR, sourceTag.getFloat(FILL_FACTOR));
            }

            // Kopiert colorLists
            for (int i = 0; i < 3; i++) {
                String key = COLOR_LIST + i;
                if (sourceTag.contains(key)) {
                    targetTag.putIntArray(key, sourceTag.getIntArray(key));
                }
            }
        }

        return target;
    }

    public static ItemStack copyColorFromLyeWater(GenericCauldronInteractions source, ItemStack target, boolean onlyAvailableInteractions) {
        CompoundTag sourceTag = source.getUpdateTag();
        CompoundTag targetTag = new CompoundTag();
        target.setTag(targetTag);

        if (!onlyAvailableInteractions) {
            // Kopiert die Hauptfarbe
            if (sourceTag.contains(COLOR_TAG)) {
                targetTag.putInt(COLOR_TAG, sourceTag.getInt(COLOR_TAG));
            }

            // Kopiert die fillFactor (Saturation Ratio)
            if (sourceTag.contains(FILL_FACTOR)) {
                targetTag.putFloat(FILL_FACTOR, sourceTag.getFloat(FILL_FACTOR));
            }

            for (int i = 0; i < 3; i++) {
                String key = COLOR_LIST + i;
                if (sourceTag.contains(key)) {
                    int[] colors = sourceTag.getIntArray(key);
                    targetTag.putIntArray(key, colors);
                }

                String countKey = COLOR_COUNT + i;
                if (sourceTag.contains(countKey)) {
                    int colorCountPerLayer = sourceTag.getInt(countKey);
                    targetTag.putInt(countKey, colorCountPerLayer);
                }
            }
        }

        if (target.is(ModItems.LYE_WATER_BUCKET.get())) {
            for (int i = 0; i < 3; i++) {
                String key = AVAILABLE_INTERACTIONS + i;
                if (sourceTag.contains(key)) {
                    targetTag.putInt(key, sourceTag.getInt(key));
                }
            }
        }

        if (target.is(ModItems.LYE_WATER_GLASS_BOTTLE.get())) {
            int totalAI = 0;
            for (int i = 0; i < 3; i++) {
                String key = AVAILABLE_INTERACTIONS + i;
                if (sourceTag.contains(key)) {

                    totalAI += sourceTag.getInt(key);
                    if (totalAI >= 3) {
                        targetTag.putInt(AVAILABLE_INTERACTIONS, 3);
                        break;
                    }
                }
            }
            targetTag.putInt(AVAILABLE_INTERACTIONS, totalAI);
        }

        return target;
    }

    public static ItemStack copyColorFromWoodenLyeWater(WoodenLyeWaterCauldronBlockEntity source, ItemStack target, boolean onlyAvailableInteractions) {
        CompoundTag sourceTag = source.getUpdateTag();
        CompoundTag targetTag = target.getOrCreateTag();

        if (!onlyAvailableInteractions) {
            // Kopiert die Hauptfarbe
            if (sourceTag.contains(COLOR_TAG)) {
                targetTag.putInt(COLOR_TAG, sourceTag.getInt(COLOR_TAG));
            }

            // Kopiert die fillFactor (Saturation Ratio)
            if (sourceTag.contains(FILL_FACTOR)) {
                targetTag.putFloat(FILL_FACTOR, sourceTag.getFloat(FILL_FACTOR));
            }

            for (int i = 0; i < 3; i++) {
                String key = COLOR_LIST + i;
                if (sourceTag.contains(key)) {
                    int[] colors = sourceTag.getIntArray(key);
                    targetTag.putIntArray(key, colors);
                }

                String countKey = COLOR_COUNT + i;
                if (sourceTag.contains(countKey)) {
                    int colorCountPerLayer = sourceTag.getInt(countKey);
                    targetTag.putInt(countKey, colorCountPerLayer);
                }
            }
        }

        if (target.is(ModItems.LYE_WATER_BUCKET.get())) {
            for (int i = 0; i < 3; i++) {
                String key = AVAILABLE_INTERACTIONS + i;
                if (sourceTag.contains(key)) {
                    targetTag.putInt(key, sourceTag.getInt(key));
                }
            }
        }

        if (target.is(ModItems.LYE_WATER_GLASS_BOTTLE.get())) {
            int totalAI = 0;
            for (int i = 0; i < 3; i++) {
                String key = AVAILABLE_INTERACTIONS + i;
                if (sourceTag.contains(key)) {

                    totalAI += sourceTag.getInt(key);
                    if (totalAI >= 3) {
                        targetTag.putInt(AVAILABLE_INTERACTIONS, 3);
                        break;
                    }
                }
            }
            targetTag.putInt(AVAILABLE_INTERACTIONS, totalAI);
        }

        return target;
    }

    public static ItemStack clearColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(COLOR_TAG)) {
            tag.remove(COLOR_TAG);
        }

        return stack;
    }

    public static int getColorSmart(ItemStack stack) {

        // 1. Wenn NBT-Farbe gesetzt → direkt zurück
        if (hasColor(stack)) {
            return getColor(stack);
        }

        // 2. Vanilla Wool / Wool Items
        if (stack.is(ItemTags.WOOL)) {
            // Mapping: Item → Block → DyeColor
            Block block = Block.byItem(stack.getItem());
            DyeColor dye = getDyeColorFromBlock(block);
            return dye != null
                    ? GenericColors.valueOf(dye.name()).getGenericColor()
                    : GenericColors.WHITE.getGenericColor();
        }

        // 3. Fallback
        return GenericColors.WHITE.getGenericColor();
    }

    public static ItemStack getDyeItemFromRGB(int rgb) {
        GenericColors closest = getClosestGenericColor(rgb);
        return new ItemStack(getDyeItemFromGenericColor(closest));
    }

    private static Item getDyeItemFromGenericColor(GenericColors color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }

    // Hilfsmethode: DyeColor aus einem Block ermitteln
    public static DyeColor getDyeColorFromBlock(Block block) {
        if (block == Blocks.WHITE_WOOL) return DyeColor.WHITE;
        if (block == Blocks.ORANGE_WOOL) return DyeColor.ORANGE;
        if (block == Blocks.MAGENTA_WOOL) return DyeColor.MAGENTA;
        if (block == Blocks.LIGHT_BLUE_WOOL) return DyeColor.LIGHT_BLUE;
        if (block == Blocks.YELLOW_WOOL) return DyeColor.YELLOW;
        if (block == Blocks.LIME_WOOL) return DyeColor.LIME;
        if (block == Blocks.PINK_WOOL) return DyeColor.PINK;
        if (block == Blocks.GRAY_WOOL) return DyeColor.GRAY;
        if (block == Blocks.LIGHT_GRAY_WOOL) return DyeColor.LIGHT_GRAY;
        if (block == Blocks.CYAN_WOOL) return DyeColor.CYAN;
        if (block == Blocks.PURPLE_WOOL) return DyeColor.PURPLE;
        if (block == Blocks.BLUE_WOOL) return DyeColor.BLUE;
        if (block == Blocks.BROWN_WOOL) return DyeColor.BROWN;
        if (block == Blocks.GREEN_WOOL) return DyeColor.GREEN;
        if (block == Blocks.RED_WOOL) return DyeColor.RED;
        if (block == Blocks.BLACK_WOOL) return DyeColor.BLACK;

        return null;
    }

    public static Block getWoolBlockFromItem(ItemStack stack) {
        int colorRGB = getColorSmart(stack); // ermittelt NBT oder Vanilla Farbe
        GenericColors closest = getClosestGenericColor(colorRGB);
        return getBlockFromGenericColor(closest);
    }

    public static GenericColors getClosestGenericColor(int rgb) {
        GenericColors best = GenericColors.WHITE;
        int bestDiff = Integer.MAX_VALUE;

        int r1 = (rgb >> 16) & 0xFF;
        int g1 = (rgb >> 8) & 0xFF;
        int b1 = rgb & 0xFF;

        for (GenericColors color : GenericColors.values()) {
            int cRGB = color.getGenericColor();

            int r2 = (cRGB >> 16) & 0xFF;
            int g2 = (cRGB >> 8) & 0xFF;
            int b2 = cRGB & 0xFF;

            int dr = r1 - r2;
            int dg = g1 - g2;
            int db = b1 - b2;

            int diff = dr * dr + dg * dg + db * db;

            if (diff < bestDiff) {
                bestDiff = diff;
                best = color;
            }
        }

        return best;
    }

    public static Block getBlockFromGenericColor(GenericColors color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_WOOL;
            case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL;
            case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL;
            case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
            case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL;
            case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL;
            case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL;
            case BLACK -> Blocks.BLACK_WOOL;
        };
    }

    public static GenericColors getGenericColorFromRGB(int rgb) {
        for (GenericColors color : GenericColors.values()) {
            if (color.getGenericColor() == rgb) {
                return color;
            }
        }
        return getClosestGenericColor(rgb);
    }

    public static String TooltipTextGenerator(ItemStack stack) {
                // Versuche den DyeColor-Namen zu bekommen
        GenericColors color = getGenericColorFromRGB(getColorSmart(stack));
        return better_string(color.name());
    }


    private static String better_string(String name) {
        if (name == null || name.isEmpty()) return name;

        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) sb.append(parts[i].substring(1).toLowerCase());
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }
}