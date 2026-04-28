package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.stream.Collectors;

public final class BobbyChestLootTables extends VanillaBlockLoot {
    public BobbyChestLootTables(HolderLookup.Provider registries) {
        super(registries);
    }

    @Override
    protected void generate() {
        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            this.dropSelf(chest.block().get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> entry.getKey().identifier().getNamespace().equals(BobbyChests.MODID))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
