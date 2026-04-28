package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public final class BobbyChestBlockTags extends BlockTagsProvider {
    public BobbyChestBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BobbyChests.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var chestBlocks = tag(BobbyChestTags.CHEST_BLOCKS);
        var mineableWithPickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            chestBlocks.add(chest.block().get());
            mineableWithPickaxe.add(chest.block().get());
        }
    }

    @Override
    public String getName() {
        return "BobbyChests Block Tags";
    }
}
