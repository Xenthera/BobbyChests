package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class BobbyChestTags {
    public static final TagKey<Block> CHEST_BLOCKS = BlockTags.create(Identifier.fromNamespaceAndPath(BobbyChests.MODID, "chests"));
    public static final TagKey<Item> CHEST_ITEMS = ItemTags.create(Identifier.fromNamespaceAndPath(BobbyChests.MODID, "chests"));

    private BobbyChestTags() {}
}
