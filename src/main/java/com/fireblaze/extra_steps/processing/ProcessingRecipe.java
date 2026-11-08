package com.fireblaze.extra_steps.processing;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProcessingRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    public record CountedIngredient(Ingredient ingredient, int count) {}
    private final List<CountedIngredient> ingredients;
    private final ItemStack result;
    private final List<Byproduct> byproducts;
    private final int time;
    private final RecipeType<ProcessingRecipe> type;
    private final ProcessingMode mode;
    private final int waterLevelConsumption;
    private final int interactionCount;
    private final List<Ingredient> tools;
    private final int ingredientAmount;

    public ProcessingRecipe(ResourceLocation id,
                            List<CountedIngredient> ingredients,
                            ItemStack result,
                            int time,
                            RecipeType<ProcessingRecipe> type,
                            ProcessingMode mode,
                            int ingredientAmount,
                            int waterLevelConsumption,
                            int interactionCount,
                            List<Ingredient> tools,
                            List<Byproduct> byproducts) {

        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.byproducts = byproducts;
        this.time = time;
        this.type = type;
        this.mode = mode;
        this.ingredientAmount = ingredientAmount;
        this.waterLevelConsumption = waterLevelConsumption;
        this.interactionCount = interactionCount;
        this.tools = tools;
    }

    public int getIngredientAmount() {
        return ingredientAmount;
    }
    public List<Byproduct> getByproducts() {
        return byproducts;
    }
    public int getTime() {
        return time;
    }

    public int getWaterLevelReduction() {
        return waterLevelConsumption;
    }
    public int getInteractionCount() {
        return interactionCount;
    }
    public List<Ingredient> getTools() {
        return tools;
    }
    public ProcessingMode getMode() {
        return mode;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {

        List<ItemStack> inputs = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }

        for (CountedIngredient ci : ingredients) {

            int needed = ci.count();
            int matched = 0;

            for (ItemStack input : inputs) {

                if (ci.ingredient().test(input)) {

                    int available = input.getCount();
                    int take = Math.min(available, needed - matched);
                    matched += take;

                    input.shrink(take);

                    if (matched >= needed) break;
                }
            }

            if (matched < needed) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess access) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();

        for (CountedIngredient ci : ingredients) {
            list.add(ci.ingredient());
        }

        return list;
    }

    public List<CountedIngredient> getCountedIngredients() {
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return type; // jetzt immer ModRecipeTypes.PROCESSING.get()
    }
    public boolean isToolValid(ItemStack stack) {

        // Rezepttools überschreiben alles
        if (tools != null && !tools.isEmpty()) {
            return tools.stream().anyMatch(tool -> tool.test(stack));
        }

        // Ingredient defaults
        if (mode.getDefaultTools().test(stack) && !stack.isEmpty()) {
            return true;
        }

        // instanceof defaults
        for (Class<? extends Item> clazz : mode.getDefaultToolClasses()) {

            if (clazz.isInstance(stack.getItem()) && !stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public static List<ItemStack> getAllowedTools(ProcessingRecipe recipe) {

        List<ItemStack> result = new ArrayList<>();

        // 1. Explizite Recipe-Tools
        if (recipe.getTools() != null && !recipe.getTools().isEmpty()) {
            for (Ingredient ing : recipe.getTools()) {
                for (ItemStack stack : ing.getItems()) {
                    result.add(stack.copy());
                }
            }

            return result;
        }

        // 2. Mode Default Ingredient Tools
        Ingredient defaultTools = recipe.getMode().getDefaultTools();
        if (defaultTools != null) {
            for (ItemStack stack : defaultTools.getItems()) {
                result.add(stack.copy());
            }
        }

        // 3. Class-based Tools (AxeItem, SwordItem etc.)
        for (Class<? extends Item> clazz : recipe.getMode().getDefaultToolClasses()) {
            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (clazz.isInstance(item)) {
                    result.add(new ItemStack(item));
                }
            }
        }

        return result;
    }

    public static Ingredient getAllowedToolsAsArray(ProcessingRecipe recipe) {

        if (recipe.getTools() != null && !recipe.getTools().isEmpty()) {
            // mehrere Ingredients → zu einem einzigen zusammenfassen
            return Ingredient.merge(recipe.getTools());
        }

        List<ItemStack> stacks = new ArrayList<>();

        Ingredient defaultTools = recipe.getMode().getDefaultTools();
        if (defaultTools != null) {
            stacks.addAll(List.of(defaultTools.getItems()));
        }

        for (Class<? extends Item> clazz : recipe.getMode().getDefaultToolClasses()) {

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (clazz.isInstance(item)) {
                    stacks.add(new ItemStack(item));
                }
            }
        }

        return Ingredient.of(stacks.stream());
    }
}