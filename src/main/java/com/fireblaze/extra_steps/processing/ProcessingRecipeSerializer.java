package com.fireblaze.extra_steps.processing;

import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessingRecipeSerializer implements RecipeSerializer<ProcessingRecipe> {

    public static final ProcessingRecipeSerializer INSTANCE = new ProcessingRecipeSerializer();

    @Override
    public ProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
        // 🔹 Ingredients (alt + neu)
        List<ProcessingRecipe.CountedIngredient> ingredients = new ArrayList<>();

        if (json.has("ingredients")) {
            // 👉 Neues System (Liste)
            for (var element : json.getAsJsonArray("ingredients")) {

                JsonObject obj = element.getAsJsonObject();

                Ingredient ingredient = Ingredient.fromJson(obj);

                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;

                ingredients.add(new ProcessingRecipe.CountedIngredient(ingredient, count));
            }
        } else if (json.has("ingredient")) {

            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            ingredients.add(new ProcessingRecipe.CountedIngredient(ingredient, 1));
        } else {
            throw new IllegalStateException("Recipe " + id + " has no 'ingredient' or 'ingredients' field!");
        }

        int ingredientAmount = json.has("ingredientAmount")
                ? json.get("ingredientAmount").getAsInt()
                : 1;

        // 🔹 Result
        ItemStack result;
        if (json.get("result").isJsonObject()) {
            JsonObject resultJson = json.getAsJsonObject("result");

            ResourceLocation itemId = ResourceLocation.tryParse(resultJson.get("item").getAsString());

            result = new ItemStack(
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(itemId))
            );

            int count = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;
            result.setCount(count);
        } else {
            String resultItem = json.get("result").getAsString();
            result = new ItemStack(
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(
                            ResourceLocation.tryParse(resultItem)
                    ))
            );
        }

        // 🔹 Zeit & Mode
        int time = json.has("time") ? json.get("time").getAsInt() : 0;
        int interactionCount = json.has("interaction_count")
                ? json.get("interaction_count").getAsInt()
                : 3;

        ProcessingMode mode = ProcessingMode.fromString(
                json.has("mode") ? json.get("mode").getAsString() : "drying"
        );

        int waterCost = json.has("water_level_consumption")
                ? json.get("water_level_consumption").getAsInt()
                : 1;

        List<Ingredient> tools = new ArrayList<>();

        if (json.has("tools")) {

            for (var element : json.getAsJsonArray("tools")) {

                JsonObject obj = element.getAsJsonObject();

                tools.add(Ingredient.fromJson(obj));
            }
        }

        List<Byproduct> byproducts = new ArrayList<>();

        if (json.has("byproducts")) {

            for (var element : json.getAsJsonArray("byproducts")) {

                JsonObject obj = element.getAsJsonObject();

                ResourceLocation itemId = ResourceLocation.tryParse(
                        obj.get("item").getAsString()
                );

                ItemStack stack = new ItemStack(
                        Objects.requireNonNull(
                                ForgeRegistries.ITEMS.getValue(itemId)
                        )
                );

                int count = obj.has("count")
                        ? obj.get("count").getAsInt()
                        : 1;

                stack.setCount(count);

                float chance = obj.has("chance")
                        ? obj.get("chance").getAsFloat()
                        : 1.0f;

                ByproductTrigger trigger = obj.has("trigger")
                        ? ByproductTrigger.fromString(obj.get("trigger").getAsString())
                        : ByproductTrigger.ON_COMPLETE;

                byproducts.add(new Byproduct(stack, chance, trigger));
            }
        }

        return new ProcessingRecipe(
                id,
                ingredients,
                result,
                time,
                ModRecipeTypes.PROCESSING.get(),
                mode,
                ingredientAmount,
                waterCost,
                interactionCount,
                tools,
                byproducts
        );
    }

    @Override
    public ProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int size = buf.readInt();

        List<ProcessingRecipe.CountedIngredient> ingredients = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Ingredient ing = Ingredient.fromNetwork(buf);
            int count = buf.readInt();

            ingredients.add(new ProcessingRecipe.CountedIngredient(ing, count));
        }

        int ingredientAmount = buf.readInt();

        ItemStack result = buf.readItem();

        int time = buf.readInt();
        int waterLevelReduction = buf.readInt();
        int interactionCount = buf.readInt();
        int toolCount = buf.readInt();
        List<Ingredient> tools = new ArrayList<>();

        for (int i = 0; i < toolCount; i++) {
            tools.add(Ingredient.fromNetwork(buf));
        }
        int byproductCount = buf.readInt();

        List<Byproduct> byproducts = new ArrayList<>();
        for (int i = 0; i < byproductCount; i++) {

            ItemStack stack = buf.readItem();
            float chance = buf.readFloat();

            ByproductTrigger trigger =
                    ByproductTrigger.fromString(buf.readUtf());

            byproducts.add(new Byproduct(stack, chance, trigger));
        }

        ProcessingMode mode = ProcessingMode.fromString(buf.readUtf());

        RecipeType<ProcessingRecipe> type = ModRecipeTypes.PROCESSING.get();

        return new ProcessingRecipe(
                id,
                ingredients,
                result,
                time,
                type,
                mode,
                ingredientAmount,
                waterLevelReduction,
                interactionCount,
                tools,
                byproducts
        );
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, ProcessingRecipe recipe) {
        buf.writeInt(recipe.getIngredients().size());

        for (ProcessingRecipe.CountedIngredient ci : recipe.getCountedIngredients()) {
            ci.ingredient().toNetwork(buf);
            buf.writeInt(ci.count());
        }

        buf.writeInt(recipe.getIngredientAmount());
        buf.writeItem(recipe.getResultItem(null));
        buf.writeInt(recipe.getTime());
        buf.writeInt(recipe.getWaterLevelReduction());
        buf.writeInt(recipe.getInteractionCount());

        buf.writeInt(recipe.getTools().size());
        for (Ingredient tool : recipe.getTools()) {
            tool.toNetwork(buf);
        }

        buf.writeInt(recipe.getByproducts().size());

        for (Byproduct byproduct : recipe.getByproducts()) {
            buf.writeItem(byproduct.stack());
            buf.writeFloat(byproduct.chance());
            buf.writeUtf(byproduct.trigger().getId());
        }

        buf.writeUtf(recipe.getMode().getId());
    }
}