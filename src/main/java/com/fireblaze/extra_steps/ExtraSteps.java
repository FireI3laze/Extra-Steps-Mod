package com.fireblaze.extra_steps;

import com.fireblaze.extra_steps.blockentity.*;
import com.fireblaze.extra_steps.cauldron.interaction.ModCauldronInteractions;
import com.fireblaze.extra_steps.cauldron.interaction.WoodenCauldronInteractions;
import com.fireblaze.extra_steps.cauldron.network.ModNetwork;
import com.fireblaze.extra_steps.client.color.*;
import com.fireblaze.extra_steps.client.render.ColorableGlassBlockRenderer;
import com.fireblaze.extra_steps.config.ModConfigHandler;
import com.fireblaze.extra_steps.fluid.*;
import com.fireblaze.extra_steps.item.LyeWaterGlassBottleItem;
import com.fireblaze.extra_steps.loot.ModLootModifiers;
import com.fireblaze.extra_steps.processing.ProcessingItemRegistry;
import com.fireblaze.extra_steps.registry.*;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExtraSteps.MODID)
public class ExtraSteps
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "extra_steps";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExtraSteps(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModTabs.register(modEventBus);

        // COMMON Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigHandler.COMMON_CONFIG);

        // CLIENT Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModConfigHandler.CLIENT_CONFIG);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Fluid-Supplier aufsetzen *nach* Block-Registrierung
        ModFluids.register(modEventBus);
        ModFluidTypes.register(modEventBus);

        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        ModRecipeSerializers.SERIALIZERS.register(modEventBus);

        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);

        ModSounds.SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        ModLootModifiers.register(modEventBus);

        ModNetwork.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModCauldronInteractions.init();
            WoodenCauldronInteractions.init();
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.COLORABLE_STAINED_GLASS_PANE.get(),
                    RenderType.translucent()
            );

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.COLORABLE_STAINED_GLASS.get(),
                    RenderType.translucent()
            );

            ItemBlockRenderTypes.setRenderLayer(ModFluids.SOURCE_LYE_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_LYE_WATER.get(), RenderType.translucent());

            Minecraft.getInstance().getBlockColors().register(
                    (state, level, pos, tintIndex) -> 0xAAC4C4CC,
                    ModBlocks.LYE_WATER_CAULDRON.get()
            );

            Minecraft.getInstance().getItemColors().register(
                    new DryingItemColor(),
                    ModItems.WET_HIDE.get(),
                    ModItems.WET_WOOL.get(),
                    ModItems.PULP_BLOCK.get());

            Minecraft.getInstance().getItemColors().register(
                    new GenericItemColoring(),
                    ModItems.CRUMBLED_WOOL.get(),
                    ModItems.RAW_WOOL.get(),
                    ModItems.FIBERED_WOOL.get(),
                    ModItems.BRUSHED_WOOL.get(),
                    ModItems.LYE_WATER_BUCKET.get()

            );

            Minecraft.getInstance().getBlockColors().register(
                    (state, level, pos, tintIndex) -> {
                        if(level != null && pos != null) {
                            var be = level.getBlockEntity(pos);
                            if(be instanceof ColorableBlockEntity block) {
                                return block.getColor();
                            }
                            if (be instanceof ColorableGlassBlockEntity block) {
                                return block.getColor();
                            }
                            if (be instanceof ColorableGlassPaneBlockEntity block) {
                                return block.getColor();
                            }
                        }
                        return 0xFFFFFF;
                    },
                    ModBlocks.COLORABLE_WOOL.get(),
                    ModBlocks.COLORABLE_STAINED_GLASS.get(),
                    ModBlocks.COLORABLE_STAINED_GLASS_PANE.get(),
                    ModBlocks.COLORABLE_TERRACOTTA.get(),
                    ModBlocks.COLORABLE_CONCRETE.get(),
                    ModBlocks.COLORABLE_CONCRETE_POWDER.get()
            );



            Minecraft.getInstance().getBlockColors().register(
                    (state, level, pos, tintIndex) -> {
                        if(level != null && pos != null) {
                            var be = level.getBlockEntity(pos);
                            if(be instanceof LyeWaterCauldronBlockEntity dyeCauldron) {
                                return dyeCauldron.getColor(); // die gemixte Farbe zurückgeben
                            } else if(be instanceof WoodenLyeWaterCauldronBlockEntity dyeCauldron) {
                                return dyeCauldron.getColor(); // die gemixte Farbe zurückgeben
                            }
                        }
                        return 0xAAC4C4CC; // default: weiß
                    },
                    ModBlocks.LYE_WATER_CAULDRON.get(),
                    ModBlocks.WOODEN_LYE_WATER_CAULDRON.get()
            );

            ItemProperties.register(ModItems.LYE_WATER_GLASS_BOTTLE.get(),
                    Objects.requireNonNull(ResourceLocation.tryParse("interactions")),
                    (stack, level, entity, seed) -> stack.hasTag()
                            ? stack.getTag().getInt(GenericColorHelper.AVAILABLE_INTERACTIONS)
                            : 0);

            Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {

                        if (stack.getItem() instanceof LyeWaterGlassBottleItem || stack.is(ModTags.COLORABLE_BLOCKS)) {
                            if (tintIndex != 0) return 0xFFFFFF;

                            // JEI Rainbow Preview
                            if (stack.hasTag() && stack.getTag().getBoolean("preview_rainbow")) {
                                long time = System.currentTimeMillis();
                                float hue = (time % 9000) / 9000f;
                                return java.awt.Color.HSBtoRGB(hue, 0.5f, 0.65f);
                            }

                            // Normal for NBT
                            if (stack.hasTag() && stack.getTag().contains(GenericColorHelper.COLOR_TAG)) {
                                return stack.getTag().getInt(GenericColorHelper.COLOR_TAG);
                            } else if (stack.is(ModItems.LYE_WATER_GLASS_BOTTLE.get())) return 0xAAC4C4CC;

                        }

                        return 0xFFFFFF;
                    }, //ModItems.LYE_WATER_BUCKET.get(),
                    ModItems.LYE_WATER_GLASS_BOTTLE.get(),
                    ModBlocks.COLORABLE_WOOL.get().asItem(),
                    ModBlocks.COLORABLE_STAINED_GLASS.get().asItem(),
                    ModBlocks.COLORABLE_STAINED_GLASS_PANE.get().asItem(),
                    ModBlocks.COLORABLE_TERRACOTTA.get().asItem(),
                    ModBlocks.COLORABLE_CONCRETE.get().asItem());

            if (ModConfigHandler.enableGlassBlockCulling.get()) {
                BlockEntityRenderers.register(ModBlockEntities.COLORABLE_GLASS_BE.get(), ColorableGlassBlockRenderer::new);
                // BlockEntityRenderers.register(ModBlockEntities.COLORABLE_GLASS_PANE_BE.get(), ColorableGlassPaneBlockRenderer::new);
            }

            Minecraft.getInstance().getBlockColors().register(
                    WoodenCauldronColors.WATER_COLOR,
                    ModBlocks.WOODEN_WATER_CAULDRON.get()
            );
        }
    }

    @Mod.EventBusSubscriber(modid = ExtraSteps.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public class ClientForgeEvents {

        private static boolean rebuilt = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (!rebuilt && Minecraft.getInstance().level != null) {
                ProcessingItemRegistry.rebuild(Minecraft.getInstance().level.getRecipeManager());
                rebuilt = true;
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ExtraSteps.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

    }
}
