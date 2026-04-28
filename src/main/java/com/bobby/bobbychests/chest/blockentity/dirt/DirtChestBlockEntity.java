package com.bobby.bobbychests.chest.blockentity.dirt;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DirtChestBlockEntity extends AbstractTieredChestBlockEntity {
    public DirtChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.DIRT_CHEST.get(), worldPosition, blockState, ChestTier.DIRT);
    }

    @Override
    public int getSlotCount() {
        return 1;
    }
}

