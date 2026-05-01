package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.registry.ModItems;
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
        tag(BobbyChestTags.UPGRADE_CARDS).add(
                ModItems.NETWORKING_UPGRADE_CARD.get(),
                ModItems.INFINITE_UPGRADE_CARD.get(),
                ModItems.VOID_UPGRADE_CARD.get(),
                ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get(),
                ModItems.RETAIN_ITEMS_UPGRADE_CARD.get(),
                ModItems.LOCK_UPGRADE_CARD.get()
        );

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            chestItems.add(chest.item());
        }
    }

    @Override
    public String getName() {
        return "BobbyChests Item Tags";
    }
}
