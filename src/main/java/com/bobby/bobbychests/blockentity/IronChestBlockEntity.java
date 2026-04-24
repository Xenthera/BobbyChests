package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.tier.ChestTier;
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

