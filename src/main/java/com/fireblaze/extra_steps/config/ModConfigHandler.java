package com.fireblaze.extra_steps.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfigHandler {

    // === Das Spec-Objekt ===
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec CLIENT_CONFIG;

    // === Konfigurationswerte (Server) ===
    public static ForgeConfigSpec.BooleanValue skipLeatherProcess;
    public static ForgeConfigSpec.BooleanValue skipWoolProcess;
    public static ForgeConfigSpec.BooleanValue enableAlwaysRawWoolDrop;
    public static ForgeConfigSpec.IntValue cauldronFillCheckInterval;
    public static ForgeConfigSpec.IntValue cauldronFillChance;

    // === Konfigurationswerte (Client) ===
    public static ForgeConfigSpec.BooleanValue enableGlassBlockCulling;
    public static ForgeConfigSpec.DoubleValue maxDistanceColorItemRendering;

    public static ForgeConfigSpec.BooleanValue enableCauldronIngredientStackSizeRendering;
    static {
        // Builder für COMMON
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();

        commonBuilder.comment("Extra Steps - General Settings");
        commonBuilder.push("Processing_Chains");

        skipLeatherProcess = commonBuilder
                .comment("Skip Leather Processing | if enabled, cows will drop leather again, therefore the processing can be skipped")
                .define("skipLeatherProcess", false);

        skipWoolProcess = commonBuilder
                .comment("Wool Processing | if enabled, sheep will drop wool blocks again, therefore the processing can be skipped")
                .define("skipWoolProcess", false);

        commonBuilder.pop();
        commonBuilder.push("Gamerules");

        enableAlwaysRawWoolDrop = commonBuilder
                .comment("if enabled, sheep will always drop raw wool, no matter if killed or sheared (this setting is ignored if enableWoolProcessing is false)")
                .define("enableAlwaysRawWoolDrop", false);

        cauldronFillCheckInterval = commonBuilder
                .comment("The interval (in ticks) the Wooden Cauldron has a chance to increase its water fill level during rain (20 ticks = 1 second)")
                .defineInRange("cauldronFillCheckInterval", 200, 0, 1200);

        cauldronFillChance = commonBuilder
                .comment("The chance the Wooden Cauldron has to increase its water fill level during rain (500 means a 1/500 chance per interval chosen above)")
                .defineInRange("cauldronFillChance", 450, 1, 10000);

        commonBuilder.pop();

        COMMON_CONFIG = commonBuilder.build();


        // Builder für CLIENT
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();

        clientBuilder.comment("Extra Steps - Client Settings");
        clientBuilder.push("Cauldron");

        maxDistanceColorItemRendering = clientBuilder
                .comment("The max distance at which color items are rendered on the Cauldron")
                .defineInRange("maxDistanceColorItemRendering", 5.0, 0.0, 1000.0);

        enableCauldronIngredientStackSizeRendering = clientBuilder
                .comment("If enabled, visualises the stack size of the ingredients in the cauldron (for mixture recipes)")
                .define("cauldronIngredientStackSizeRenderingIsEnabled", true);

        clientBuilder.pop();
        clientBuilder.push("Deco_Blocks");

        enableGlassBlockCulling = clientBuilder
                .comment("[EXPERIMENTAL] Glass Block Culling - Removes the inner lines of the glass block -> makes them invisible when looking through transparent blocks")
                .define("glassBlockCullingIsEnabled", false);

        clientBuilder.pop();

        CLIENT_CONFIG = clientBuilder.build();
    }
}