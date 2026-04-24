package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class DiamondChestBlockEntity extends AbstractTieredChestBlockEntity {
    public DiamondChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.DIAMOND_CHEST.get(), worldPosition, blockState, ChestTier.DIAMOND);
    }

    @Override
    public int getSlotCount() {
        return 18 * 6; // 108
    }
}

