package com.bobby.bobbychests.item;

import net.minecraft.world.item.ItemStack;

public final class CreativeInfiniteUpgradeCardItem extends ChestUpgradeCardItem {
    public CreativeInfiniteUpgradeCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
