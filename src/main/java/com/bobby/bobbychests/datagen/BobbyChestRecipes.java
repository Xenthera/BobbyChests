package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

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

        this.shapeless(RecipeCategory.MISC, ModItems.UPGRADE_ALLOY_BLEND.get())
                .requires(Items.COPPER_INGOT)
                .requires(Items.IRON_NUGGET)
                .requires(Items.REDSTONE)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_redstone", this.has(Items.REDSTONE))
                .save(this.output);

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModItems.UPGRADE_ALLOY_BLEND.get()),
                        RecipeCategory.MISC,
                        CookingBookCategory.MISC,
                        ModItems.UPGRADE_ALLOY.get(),
                        0.35F,
                        200
                )
                .unlockedBy("has_upgrade_alloy_blend", this.has(ModItems.UPGRADE_ALLOY_BLEND.get()))
                .save(this.output);

        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModItems.UPGRADE_ALLOY_BLEND.get()),
                        RecipeCategory.MISC,
                        CookingBookCategory.MISC,
                        ModItems.UPGRADE_ALLOY.get(),
                        0.35F,
                        100
                )
                .unlockedBy("has_upgrade_alloy_blend", this.has(ModItems.UPGRADE_ALLOY_BLEND.get()))
                .save(this.output, "bobbychests:upgrade_alloy_from_blasting");

        this.shaped(RecipeCategory.MISC, ModItems.BLANK_UPGRADE_CARD.get())
                .pattern("PRP")
                .pattern("PAP")
                .pattern("PRP")
                .define('A', ModItems.UPGRADE_ALLOY.get())
                .define('P', Items.PAPER)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_upgrade_alloy", this.has(ModItems.UPGRADE_ALLOY.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.LOCK_UPGRADE_CARD.get())
                .pattern(" I ")
                .pattern("IBI")
                .pattern(" I ")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get())
                .pattern("RCR")
                .pattern("HBH")
                .pattern("RCR")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('C', Items.COMPARATOR)
                .define('H', Items.HOPPER)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.VOID_UPGRADE_CARD.get())
                .pattern("OMO")
                .pattern("RBR")
                .pattern("OMO")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('M', Blocks.MAGMA_BLOCK)
                .define('O', Items.OBSIDIAN)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.RETAIN_ITEMS_UPGRADE_CARD.get())
                .pattern("EDE")
                .pattern("UBU")
                .pattern("EDE")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('D', Items.DIAMOND)
                .define('E', Items.ENDER_PEARL)
                .define('U', Items.BUNDLE)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.NETWORKING_UPGRADE_CARD.get())
                .pattern("EGE")
                .pattern("CBC")
                .pattern("RGR")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('C', Items.COMPARATOR)
                .define('E', Items.ENDER_PEARL)
                .define('G', Items.GOLD_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
                .save(this.output);

        this.shaped(RecipeCategory.MISC, ModItems.DEEP_STORAGE_UPGRADE_CARD.get())
                .pattern("ODO")
                .pattern("CBC")
                .pattern("ODO")
                .define('B', ModItems.BLANK_UPGRADE_CARD.get())
                .define('C', Blocks.BARREL)
                .define('D', Items.DIAMOND)
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_blank_upgrade_card", this.has(ModItems.BLANK_UPGRADE_CARD.get()))
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
