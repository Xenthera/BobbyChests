package com.bobby.bobbychests.chest.blockentity.iron;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IronChestBlockEntity extends AbstractTieredChestBlockEntity {
    public IronChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.IRON_CHEST.get(), worldPosition, blockState, ChestTier.IRON);
    }

    @Override
    public int getSlotCount() {
        return 27;
    }
}

