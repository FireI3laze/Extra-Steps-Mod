package com.fireblaze.extra_steps.compat;

import com.fireblaze.extra_steps.ExtraSteps;
import com.fireblaze.extra_steps.processing.Byproduct;
import com.fireblaze.extra_steps.processing.ByproductTrigger;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.registry.ModBlocks;
import com.fireblaze.extra_steps.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ProcessingRecipeCategory implements IRecipeCategory<ProcessingRecipe> {
    private final ProcessingMode mode;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable sun;
    private final IGuiHelper helper;
    private final ItemStack wooden_cauldron;
    private final ItemStack basket;
    public ProcessingRecipeCategory(IGuiHelper helper, ProcessingMode mode) {
        this.helper = helper;
        this.mode = mode;
        this.background = helper.createBlankDrawable(120, 50); // etwas größer für Pfeil + Zeit
        this.icon = helper.createDrawableItemStack(JEIHelper.getIconForMode(mode));

        // Pfeil Drawable
        ResourceLocation furnaceGui = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
        this.arrow = helper.createDrawable(furnaceGui, 176, 14, 24, 17);

        ResourceLocation mySunTexture = ResourceLocation.fromNamespaceAndPath(ExtraSteps.MODID, "textures/gui/lye_water_bottle.png");
        this.sun = helper.createDrawableItemStack(new ItemStack(Items.SUNFLOWER));
        this.wooden_cauldron = new ItemStack(ModBlocks.WOODEN_CAULDRON.get());
        this.basket = new ItemStack(ModBlocks.BASKET.get());
    }

    public static ResourceLocation getUid(ProcessingMode mode) {
        return ResourceLocation.fromNamespaceAndPath(ExtraSteps.MODID, "processing_" + mode.getId());
    }

    @Override
    public RecipeType<ProcessingRecipe> getRecipeType() {
        return getRecipeType(mode);
    }

    @Override
    public Component getTitle() {
        return switch (mode) {
            case DRYING -> Component.literal("Drying Rack");
            case BRUSHING -> Component.literal("Brushing");
            case COMPRESSING -> Component.literal("Compressing");
            case SCRAPING -> Component.literal("Scraping");
            case CLEANING -> Component.literal("Cleaning & Dying");
            case MIXING -> Component.literal("Mixing");
            case LYE_WATER -> Component.literal("Lye Water");
        };
    }

    public static RecipeType<ProcessingRecipe> getRecipeType(ProcessingMode mode) {
        return new RecipeType<>(
                getUid(mode),
                ProcessingRecipe.class
        );
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessingRecipe recipe, IFocusGroup focuses) {

        if (mode == ProcessingMode.MIXING) {
            List<Ingredient> ingredients = recipe.getIngredients();
            int[][] slots = {
                    {15, 16},
                    {0, 16},
                    {30, 16},
                    {0, 0},
                    {15, 0},
                    {30, 0},
                    {0, 32},
                    {15, 32}
            };

            List<ProcessingRecipe.CountedIngredient> ingredientCount = recipe.getCountedIngredients();

            for (int i = 0; i < ingredients.size() && i < slots.length; i++) {

                ProcessingRecipe.CountedIngredient ci = ingredientCount.get(i);

                ItemStack stack = ci.ingredient().getItems()[0].copy();
                stack.setCount(ci.count());

                builder.addSlot(RecipeIngredientRole.INPUT, slots[i][0], slots[i][1])
                        .addItemStack(stack);
            }
            toolInformation(builder, recipe, "Use to stir the mixture", 50, 32);
            cauldronInformation(builder, recipe, "Put ingredients in Water Cauldron", 50, 0);


            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, 85, 16)
                    .addItemStack(recipe.getResultItem(null));
        }
        else if (mode == ProcessingMode.LYE_WATER) {
            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, 20, 16)
                    .addIngredients(recipe.getIngredients().get(0));

            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, 80, 16)
                    .addItemStack(new ItemStack(ModItems.LYE_WATER_BUCKET.get()));

            cauldronInformation(builder, recipe, "Interact with Water Cauldron", 50, 0);
        }
        else if (mode == ProcessingMode.COMPRESSING) {
            List<ProcessingRecipe.CountedIngredient> ingredientCount = recipe.getCountedIngredients();
            ProcessingRecipe.CountedIngredient ci = ingredientCount.get(0);

            ItemStack stack = ci.ingredient().getItems()[0].copy();
            stack.setCount(ci.count());

            builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
                    .addItemStack(stack);

            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, 80, 16)
                    .addItemStack(recipe.getResultItem(null));
        }
        else {
            List<ProcessingRecipe.CountedIngredient> ingredientCount = recipe.getCountedIngredients();

            ProcessingRecipe.CountedIngredient ci = ingredientCount.get(0);

            ItemStack stack = ci.ingredient().getItems()[0].copy();
            stack.setCount(ci.count());

            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, 20, 16)
                    .addIngredients(recipe.getIngredients().get(0));

            builder.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, 80, 16)
                    .addItemStack(recipe.getResultItem(null));

            if (mode == ProcessingMode.BRUSHING || mode == ProcessingMode.SCRAPING) {
                toolInformation(builder, recipe, "Interact on Item in Drying Rack", 50, 0);
            }
        }
        if (mode == ProcessingMode.CLEANING || mode == ProcessingMode.MIXING) {
            cauldronInformation(builder, recipe, "Interact with Lye-Water Cauldron", 50, 0);
        }

        addByproductSlots(builder, recipe);
    }

    @Override
    public void draw(ProcessingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
        int arrowX = 46;
        int arrowY = 16;

        // Pfeil rendern zwischen Input und Output
        arrow.draw(gui, arrowX, arrowY);
        // draw-Methode
        int iconX = arrowX + 4;
        int iconY = arrowY - 16;

        // Zeit über Pfeil anzeigen, wenn Maus darüber
        int arrowWidth = 24;
        int arrowHeight = 16;

        if (recipe.getTime() > 0 && mouseX >= arrowX && mouseX <= arrowX + arrowWidth &&
                mouseY >= arrowY && mouseY <= arrowY + arrowHeight) {
            Font font = Minecraft.getInstance().font;
            gui.renderTooltip(
                    font,
                    Component.literal("Time: " + (recipe.getTime() / 20f) + "s"),
                    (int) mouseX,
                    (int) mouseY
            );
        }

        switch (mode) {
            case DRYING -> sun.draw(gui, iconX, iconY);
            //case SCRAPING, BRUSHING -> gui.renderItem(Objects.requireNonNull(drawRequiredTools(recipe)), iconX, iconY);
            //case MIXING, CLEANING, LYE_WATER -> gui.renderItem(Objects.requireNonNull(drawCauldrons()), iconX, iconY);
            case COMPRESSING -> gui.renderItem(basket, iconX, iconY);
        }

        /*
        if (recipe.getMode() == ProcessingMode.MIXING) {
            gui.renderItem(Objects.requireNonNull(drawRequiredTools(recipe)), iconX, iconY + 32);
        }
        */

        if (mouseX >= iconX && mouseX <= iconX + arrowWidth &&
                mouseY >= iconY && mouseY <= iconY + arrowHeight) {

            List<Component> tooltipLines = null;

            switch (mode) {
                case DRYING -> tooltipLines = List.of(
                        Component.literal("Factors like biomes, daytime"),
                        Component.literal("and weather can influence the duration")
                );
                /*
                case SCRAPING -> tooltipLines = List.of(
                        Component.literal("Interact on Item in Drying Rack")
                );
                case BRUSHING -> tooltipLines = List.of(
                        Component.literal("Hold interact on Item in Drying Rack")
                );
                */
                case COMPRESSING -> tooltipLines = List.of(
                        Component.literal("Jump on the Basket")
                );
                /*
                case CLEANING -> tooltipLines = List.of(
                        Component.literal("Interact with Lye Water Cauldron")
                );
                case MIXING -> tooltipLines = List.of(
                        Component.literal("Put ingredients in Water Cauldron")
                );
                case LYE_WATER -> tooltipLines = List.of(
                        Component.literal("Interact with Water Cauldron")
                );
                */
            }

            if (tooltipLines != null) renderMultilineTooltip(gui, Minecraft.getInstance().font, tooltipLines, (int) mouseX, (int) mouseY);
        }
    }

    private Ingredient allowedCauldrons() {
        return Ingredient.of(
                ModBlocks.WOODEN_CAULDRON.get(),
                Blocks.CAULDRON
        );
    }

    private ItemStack drawCauldrons() {
        List<ItemStack> cauldrons = List.of(
                new ItemStack(ModBlocks.WOODEN_CAULDRON.get()),
                new ItemStack(Blocks.CAULDRON)
        );

        int index = (int) (Minecraft.getInstance().level.getGameTime() / 20 % cauldrons.size());
        return cauldrons.get(index);
    }

    private ItemStack drawRequiredTools(ProcessingRecipe recipe) {
        List<ItemStack> tools = ProcessingRecipe.getAllowedTools(recipe);
        int index = (int) (Minecraft.getInstance().level.getGameTime() / 20 % tools.size());
        return tools.get(index);
    }

    private void toolInformation(IRecipeLayoutBuilder builder, ProcessingRecipe recipe, String text, int x, int y) {
        List<ProcessingRecipe.CountedIngredient> ingredientCount = recipe.getCountedIngredients();

        ProcessingRecipe.CountedIngredient ci = ingredientCount.get(0);

        ItemStack stack = ci.ingredient().getItems()[0].copy();
        stack.setCount(ci.count());

        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                .addIngredients(ProcessingRecipe.getAllowedToolsAsArray(recipe))
                .addTooltipCallback((ingredient, tooltip) -> {
                    tooltip.add(Component.literal(text));
                });
    }

    private void cauldronInformation(IRecipeLayoutBuilder builder, ProcessingRecipe recipe, String text, int x, int y) {
        List<ProcessingRecipe.CountedIngredient> ingredientCount = recipe.getCountedIngredients();

        ProcessingRecipe.CountedIngredient ci = ingredientCount.get(0);

        ItemStack stack = ci.ingredient().getItems()[0].copy();
        stack.setCount(ci.count());

        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                .addIngredients(allowedCauldrons())
                .addTooltipCallback((ingredient, tooltip) -> {
                    tooltip.add(Component.literal(text));
                });
    }

    public static void renderMultilineTooltip(GuiGraphics gui, Font font, List<Component> lines, int mouseX, int mouseY) {
        int lineHeight = 13; // Abstand zwischen den Zeilen
        int yOffset = 0;

        for (Component line : lines) {
            gui.renderTooltip(font, line, mouseX, mouseY + yOffset);
            yOffset += lineHeight;
        }
    }

    @Override
    public boolean isHandled(ProcessingRecipe recipe) {
        return recipe.getMode() == mode;
    }

    private void addByproductSlots(
            IRecipeLayoutBuilder builder,
            ProcessingRecipe recipe
    ) {

        int x = 102;

        int baseY = 16;
        int step = 18;

        List<Byproduct> byproducts = recipe.getByproducts();

        int count = byproducts.size();
        int centerOffset = (count % 2 == 0) ? (-step / 2) : 0;

        for (int i = 0; i < count; i++) {

            Byproduct byproduct = byproducts.get(i);

            int offsetIndex = getAlternatingOffset(i);

            int y = baseY + (offsetIndex * step) + centerOffset;

            builder.addSlot(
                            RecipeIngredientRole.OUTPUT,
                            x,
                            y
                    )
                    .addItemStack(byproduct.stack())
                    .addTooltipCallback((view, tooltip) -> {

                        int percent = (int) (byproduct.chance() * 100);

                        tooltip.add(
                                Component.literal("Chance: " + percent + "%")
                                        .withStyle(ChatFormatting.YELLOW)
                        );

                        tooltip.add(
                                Component.literal(
                                        "Trigger: " +
                                                switch (byproduct.trigger()) {
                                                    case ON_COMPLETE -> "On Complete";
                                                    case PER_INTERACTION -> "Per Interaction";
                                                }
                                ).withStyle(
                                        byproduct.trigger() == ByproductTrigger.ON_COMPLETE
                                                ? ChatFormatting.GREEN
                                                : ChatFormatting.GOLD
                                )
                        );
                    });
        }
    }
    private int getAlternatingOffset(int i) {

        if (i == 0) return 0;

        int n = (i + 1) / 2;

        return (i % 2 == 1) ? n : -n;
    }
}
