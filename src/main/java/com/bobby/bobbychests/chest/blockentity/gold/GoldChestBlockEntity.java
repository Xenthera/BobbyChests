package com.bobby.bobbychests.chest.blockentity.gold;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class GoldChestBlockEntity extends AbstractTieredChestBlockEntity {
    public GoldChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.GOLD_CHEST.get(), worldPosition, blockState, ChestTier.GOLD);
    }

    @Override
    public int getSlotCount() {
        return 54;
    }
}

