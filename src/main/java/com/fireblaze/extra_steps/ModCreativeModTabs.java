package com.fireblaze.extra_steps;

import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MOD_TABS =DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExtraSteps.MODID);

    public static final RegistryObject<CreativeModeTab> EXTRA_STEPS_TAB = CREATIVE_MOD_TABS.register("extra_steps_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COW_HIDE.get()))
                    .title(Component.translatable("creative_tab.extra_steps_tab"))
                    .displayItems(((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.CRUMBLED_WOOL.get());
                        pOutput.accept(ModItems.RAW_WOOL.get());
                        pOutput.accept(ModItems.WET_WOOL.get());
                        pOutput.accept(ModItems.FIBERED_WOOL.get());
                        pOutput.accept(ModItems.BRUSHED_WOOL.get());
                        pOutput.accept(ModItems.WOOL_BRUSH.get());

                        pOutput.accept(ModItems.HIDE.get());
                        pOutput.accept(ModItems.COW_HIDE.get());
                        pOutput.accept(ModItems.PIG_HIDE.get());
                        pOutput.accept(ModItems.WOLF_PELT.get());
                        pOutput.accept(ModItems.FOX_PELT.get());
                        pOutput.accept(ModItems.POLAR_BEAR_PELT.get());
                        pOutput.accept(ModItems.PANDA_PELT.get());
                        pOutput.accept(ModItems.EQUINE_HIDE.get());
                        pOutput.accept(ModItems.LLAMA_HIDE.get());
                        pOutput.accept(ModItems.SCRAPED_HIDE.get());
                        pOutput.accept(ModItems.WET_HIDE.get());

                        pOutput.accept(ModItems.PLANT_FIBER.get());
                        pOutput.accept(ModItems.PULP.get());
                        pOutput.accept(ModItems.PULP_BLOCK.get());
                        pOutput.accept(ModItems.PAPER_BLOCK.get());

                        pOutput.accept(ModItems.ASH.get());
                        pOutput.accept(ModItems.LYE_WATER_BUCKET.get());;
                        pOutput.accept(ModItems.LYE_WATER_GLASS_BOTTLE.get());

                        pOutput.accept(ModBlocks.DRYING_RACK.get());
                        //pOutput.accept(ModBlocks.HANGING_DRYING_RACK.get());
                        pOutput.accept(ModBlocks.BASKET.get());
                        //pOutput.accept(ModBlocks.SPINNING_WHEEL.get());
                        pOutput.accept(ModBlocks.WOODEN_CAULDRON.get());
                        pOutput.accept(ModBlocks.COLORABLE_WOOL.get());
                        pOutput.accept(ModBlocks.COLORABLE_STAINED_GLASS.get());
                        pOutput.accept(ModBlocks.COLORABLE_STAINED_GLASS_PANE.get());
                        pOutput.accept(ModBlocks.COLORABLE_TERRACOTTA.get());
                        pOutput.accept(ModBlocks.COLORABLE_CONCRETE.get());
                    }))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MOD_TABS.register(eventBus);
    }
}
