package com.bobby.bobbychests.chest.blockentity.netherite;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class NetheriteChestBlockEntity extends AbstractTieredChestBlockEntity {
    public NetheriteChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.NETHERITE_CHEST.get(), worldPosition, blockState, ChestTier.NETHERITE);
    }

    @Override
    protected boolean shouldResetScrollableMenuOnStorageKeyChange() {
        return true;
    }

    @Override
    public int getSlotCount() {
        return 9 * 36; // 324
    }
}
