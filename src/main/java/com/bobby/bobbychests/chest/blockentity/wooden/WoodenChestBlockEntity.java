package com.bobby.bobbychests.chest.blockentity.wooden;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
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

    @Override
    protected boolean supportsDeepStorage() {
        return true;
    }
}

