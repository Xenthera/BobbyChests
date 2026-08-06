package com.bobby.bobbychests.item;

import net.minecraft.world.item.Item;

/**
 * A card that changes what a chest stores, rather than adding a behaviour to it.
 *
 * <p>A distinct type from {@link ChestUpgradeCardItem} so the two can never be mixed up. Mode cards
 * live in their own single slot and do not compete with upgrades for space: a dirt chest has one
 * upgrade slot, and turning it into a tank should not consume it.
 */
public class ChestModeCardItem extends Item {
    public ChestModeCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
