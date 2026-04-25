package com.bobby.bobbychests.blockentity.emerald;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class EmeraldChestBlockEntity extends AbstractTieredChestBlockEntity {
    public EmeraldChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.EMERALD_CHEST.get(), worldPosition, blockState, ChestTier.EMERALD);
    }

    @Override
    protected boolean shouldResetScrollableMenuOnStorageKeyChange() {
        return true;
    }

    @Override
    public int getSlotCount() {
        return 18 * 12; // 216
    }
}
