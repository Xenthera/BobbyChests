package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.block.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;

final class BobbyChestData {
    static final List<ChestDefinition> CHESTS = List.of(
            new ChestDefinition("dirt_chest", "Dirt Chest", ModBlocks.DIRT_CHEST, Items.DIRT, Items.CHEST, "minecraft:block/dirt"),
            new ChestDefinition("wooden_chest", "Wooden Chest", ModBlocks.WOODEN_CHEST, Items.OAK_PLANKS, Items.CHEST, "minecraft:block/oak_planks"),
            new ChestDefinition("copper_chest", "Copper Chest", ModBlocks.COPPER_CHEST, Items.COPPER_INGOT, Items.CHEST, "minecraft:block/copper_block"),
            new ChestDefinition("iron_chest", "Iron Chest", ModBlocks.IRON_CHEST, Items.IRON_INGOT, Items.CHEST, "minecraft:block/iron_block"),
            new ChestDefinition("gold_chest", "Gold Chest", ModBlocks.GOLD_CHEST, Items.GOLD_INGOT, Items.CHEST, "minecraft:block/gold_block"),
            new ChestDefinition("diamond_chest", "Diamond Chest", ModBlocks.DIAMOND_CHEST, Items.DIAMOND, Items.CHEST, "minecraft:block/diamond_block"),
            new ChestDefinition("emerald_chest", "Emerald Chest", ModBlocks.EMERALD_CHEST, Items.EMERALD, Items.CHEST, "minecraft:block/emerald_block"),
            new ChestDefinition("netherite_chest", "Netherite Chest", ModBlocks.NETHERITE_CHEST, Items.NETHERITE_INGOT, Items.CHEST, "minecraft:block/netherite_block")
    );

    private BobbyChestData() {}

    record ChestDefinition(String id, String displayName, DeferredBlock<? extends Block> block, ItemLike recipeIngredient, ItemLike recipeCore, String particleTexture) {
        Item item() {
            return this.block.get().asItem();
        }
    }
}
