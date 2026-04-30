package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

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

        this.shaped(RecipeCategory.MISC, ModItems.NETWORKING_UPGRADE_CARD.get())
                .pattern("PPP")
                .pattern("PCP")
                .pattern("PPP")
                .define('C', BobbyChestTags.CHEST_ITEMS)
                .define('P', Items.PAPER)
                .unlockedBy("has_bobby_chest", this.has(BobbyChestTags.CHEST_ITEMS))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.INFINITE_UPGRADE_CARD.get())
                .pattern("PEP")
                .pattern("ECE")
                .pattern("PEP")
                .define('C', BobbyChestTags.CHEST_ITEMS)
                .define('E', Items.ENDER_PEARL)
                .define('P', Items.PAPER)
                .unlockedBy("has_bobby_chest", this.has(BobbyChestTags.CHEST_ITEMS))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.VOID_UPGRADE_CARD.get())
                .pattern("PPP")
                .pattern("PCP")
                .pattern("POP")
                .define('C', BobbyChestTags.CHEST_ITEMS)
                .define('O', Items.OBSIDIAN)
                .define('P', Items.PAPER)
                .unlockedBy("has_bobby_chest", this.has(BobbyChestTags.CHEST_ITEMS))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get())
                .pattern("PSP")
                .pattern("PCP")
                .pattern("PSP")
                .define('C', BobbyChestTags.CHEST_ITEMS)
                .define('P', Items.PAPER)
                .define('S', Items.STICK)
                .unlockedBy("has_bobby_chest", this.has(BobbyChestTags.CHEST_ITEMS))
                .save(this.output);
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
