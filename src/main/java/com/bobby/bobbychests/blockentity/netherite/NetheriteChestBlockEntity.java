package com.bobby.bobbychests.blockentity.netherite;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.tier.ChestTier;
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
        return 18 * 18;
    }
}
