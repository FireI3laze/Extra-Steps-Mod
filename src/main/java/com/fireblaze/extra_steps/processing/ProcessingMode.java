package com.fireblaze.extra_steps.processing;

import com.fireblaze.extra_steps.registry.ModItems;
import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import com.fireblaze.extra_steps.registry.ModTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.Optional;

public enum ProcessingMode {

    DRYING("drying"),
    BRUSHING("brushing", Ingredient.of(ModItems.WOOL_BRUSH.get())),
    COMPRESSING("compressing"),
    SCRAPING(
            "scraping",
            Ingredient.EMPTY,
            SwordItem.class,
            AxeItem.class
    ),

    //SPINNING("spinning"),
    CLEANING("cleaning"),
    MIXING(
            "mixing",
            Ingredient.EMPTY,
            ShovelItem.class
    ),
    LYE_WATER("lye_water");

    private final String id;
    private final Ingredient defaultTools;
    private final Class<? extends Item>[] defaultToolClasses;

    ProcessingMode(String id) {
        this(id, Ingredient.EMPTY);
    }

    ProcessingMode(String id, Ingredient defaultTools, Class<? extends Item>... defaultToolClasses) {
        this.id = id;
        this.defaultTools = defaultTools;
        this.defaultToolClasses = defaultToolClasses;
    }

    public String getId() {
        return id;
    }

    public static ProcessingMode fromString(String id) {
        for (ProcessingMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown processing mode: " + id);
    }

    public Ingredient getDefaultTools() {
        return defaultTools;
    }

    public Class<? extends Item>[] getDefaultToolClasses() {
        return defaultToolClasses;
    }
}