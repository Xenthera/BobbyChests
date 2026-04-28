package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class DataGenerators {
    private DataGenerators() {}

    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, new BobbyChestAssetProvider(packOutput));
        generator.addProvider(true, new BobbyChestLanguageProvider(packOutput, "en_us"));
        generator.addProvider(true, new BobbyChestRecipes.Runner(packOutput, lookupProvider));
        generator.addProvider(true, new LootTableProvider(
                packOutput,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BobbyChestLootTables::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        generator.addProvider(true, new BobbyChestBlockTags(packOutput, lookupProvider));
        generator.addProvider(true, new BobbyChestItemTags(packOutput, lookupProvider));
    }
}
