package com.bobby.bobbychests.blockentity.gold;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;

import com.bobby.bobbychests.tier.ChestTier;
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

