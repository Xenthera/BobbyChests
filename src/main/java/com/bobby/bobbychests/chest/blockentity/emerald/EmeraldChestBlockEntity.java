package com.bobby.bobbychests.chest.blockentity.emerald;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
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

}
