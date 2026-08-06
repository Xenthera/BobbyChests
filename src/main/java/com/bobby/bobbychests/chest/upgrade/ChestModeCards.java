package com.bobby.bobbychests.chest.upgrade;

import com.bobby.bobbychests.datagen.BobbyChestTags;
import com.bobby.bobbychests.item.ChestModeCardItem;
import net.minecraft.world.item.ItemStack;

/**
 * Tells mode cards apart from upgrade cards.
 *
 * <p>The two go in different slots and never mix: a mode card changes what the chest stores, an
 * upgrade card changes how it behaves. Keeping the test in one place means the slot restrictions,
 * the deny tooltips, and the capability derivation all agree about which is which.
 */
public final class ChestModeCards {

    private ChestModeCards() {
    }

    public static boolean isModeCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof ChestModeCardItem || stack.is(BobbyChestTags.MODE_CARDS);
    }
}
