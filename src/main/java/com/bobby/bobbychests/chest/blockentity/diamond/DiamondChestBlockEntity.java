package com.bobby.bobbychests.chest.blockentity.diamond;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class DiamondChestBlockEntity extends AbstractTieredChestBlockEntity {
    public DiamondChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.DIAMOND_CHEST.get(), worldPosition, blockState, ChestTier.DIAMOND);
    }

}

