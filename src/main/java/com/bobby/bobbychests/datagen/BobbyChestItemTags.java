package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public final class BobbyChestItemTags extends ItemTagsProvider {
    public BobbyChestItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BobbyChests.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var chestItems = tag(BobbyChestTags.CHEST_ITEMS);
        tag(BobbyChestTags.UPGRADE_CARDS);

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            chestItems.add(chest.item());
        }
    }

    @Override
    public String getName() {
        return "BobbyChests Item Tags";
    }
}
