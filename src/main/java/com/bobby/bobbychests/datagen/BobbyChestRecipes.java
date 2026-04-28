package com.bobby.bobbychests.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public final class BobbyChestRecipes extends RecipeProvider {
    private BobbyChestRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            this.shaped(RecipeCategory.DECORATIONS, chest.block().get())
                    .pattern("MMM")
                    .pattern("MCM")
                    .pattern("MMM")
                    .define('M', chest.recipeIngredient())
                    .define('C', chest.recipeCore())
                    .unlockedBy("has_" + chest.id() + "_ingredient", this.has(chest.recipeIngredient()))
                    .save(this.output);
        }
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new BobbyChestRecipes(registries, output);
        }

        @Override
        public String getName() {
            return "BobbyChests Recipes";
        }
    }
}
