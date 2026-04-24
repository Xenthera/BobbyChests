package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.tier.ChestTier;
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

