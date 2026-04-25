package com.bobby.bobbychests.blockentity.wooden;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class WoodenChestBlockEntity extends AbstractTieredChestBlockEntity {
    public WoodenChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.WOODEN_CHEST.get(), worldPosition, blockState, ChestTier.WOOD);
    }

    @Override
    public int getSlotCount() {
        return 9;
    }
}

