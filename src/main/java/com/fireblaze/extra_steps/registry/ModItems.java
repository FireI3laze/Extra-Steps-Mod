package com.fireblaze.extra_steps.registry;

import com.fireblaze.extra_steps.ExtraSteps;
import com.fireblaze.extra_steps.fluid.ModFluids;
import com.fireblaze.extra_steps.item.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExtraSteps.MODID);

    // Wool system
    public static final RegistryObject<Item> CRUMBLED_WOOL =
            ITEMS.register("crumbled_wool", () -> new ColoredWoolItem(new Item.Properties()));

    public static final RegistryObject<Item> RAW_WOOL =
            ITEMS.register("raw_wool", () -> new ColoredWoolItem(new Item.Properties()));

    public static final RegistryObject<Item> WET_WOOL =
            ITEMS.register("wet_wool", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIBERED_WOOL =
            ITEMS.register("fibered_wool", () -> new ColoredWoolItem(new Item.Properties()));

    public static final RegistryObject<Item> BRUSHED_WOOL =
            ITEMS.register("brushed_wool", () -> new ColoredWoolItem(new Item.Properties()));

    // Tool
    public static final RegistryObject<Item> WOOL_BRUSH =
            ITEMS.register("wool_brush", () -> new WoolBrushItem(new Item.Properties().durability(900)));

    // Ash
    public static final RegistryObject<Item> ASH =
            ITEMS.register("ash", () -> new Item(new Item.Properties()));

    // Leather System
    public static final RegistryObject<Item> HIDE =
            ITEMS.register("raw_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COW_HIDE =
            ITEMS.register("raw_cow_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PIG_HIDE =
            ITEMS.register("raw_pig_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WOLF_PELT =
            ITEMS.register("raw_wolf_pelt", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FOX_PELT =
            ITEMS.register("raw_fox_pelt", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> POLAR_BEAR_PELT =
            ITEMS.register("raw_polar_bear_pelt", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PANDA_PELT =
            ITEMS.register("raw_panda_pelt", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> EQUINE_HIDE =
            ITEMS.register("raw_equine_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LLAMA_HIDE =
            ITEMS.register("raw_llama_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SCRAPED_HIDE =
            ITEMS.register("scraped_hide", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WET_HIDE =
            ITEMS.register("wet_hide", () -> new Item(new Item.Properties()));

    // Paper Production
    public static final RegistryObject<Item> PLANT_FIBER =
            ITEMS.register("plant_fiber", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PULP =
            ITEMS.register("pulp", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PULP_BLOCK =
            ITEMS.register("pulp_block", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PAPER_BLOCK =
            ITEMS.register("paper_block", () -> new NoEnchantItem(new Item.Properties()
                    .durability(8)));

    public static final RegistryObject<Item> LYE_WATER_GLASS_BOTTLE =
            ITEMS.register("lye_water_glass_bottle", () -> new LyeWaterGlassBottleItem(new Item.Properties()));

    public static final RegistryObject<Item> LYE_WATER_BUCKET =
            ITEMS.register("lye_water_bucket",
                    () -> new LyeWaterBucketItem(
                            ModFluids.SOURCE_LYE_WATER,
                            new Item.Properties().stacksTo(1)
                    ));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}