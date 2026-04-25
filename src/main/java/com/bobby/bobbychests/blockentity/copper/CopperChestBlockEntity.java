package com.bobby.bobbychests.blockentity.copper;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CopperChestBlockEntity extends AbstractTieredChestBlockEntity {
    public CopperChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.COPPER_CHEST.get(), worldPosition, blockState, ChestTier.COPPER);
    }

    @Override
    public int getSlotCount() {
        return 18;
    }
}

