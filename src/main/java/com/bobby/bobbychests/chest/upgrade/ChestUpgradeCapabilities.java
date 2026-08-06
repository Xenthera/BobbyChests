package com.bobby.bobbychests.chest.upgrade;

import com.bobby.bobbychests.chest.storage.ChestResourceMode;

/**
 * Feature flags derived from installed upgrade cards ({@link ChestUpgradeManager#capabilities()}).
 *
 * @param canUseGlobalPooledStorage true when a networking upgrade is present in any upgrade slot
 * @param storesFluid               true when a fluid upgrade is present, turning the chest into a tank
 * @param storesEnergy              true when an energy upgrade is present, turning the chest into an FE buffer
 */
public record ChestUpgradeCapabilities(
        boolean canUseGlobalPooledStorage,
        boolean canExtractInfinitely,
        boolean canVoidWhenFull,
        boolean canLeaveLastItemForAutomation,
        boolean canRetainItemsOnBreak,
        boolean canUseLocking,
        boolean canUseDeepStorage,
        boolean storesFluid,
        boolean storesEnergy) {

    /**
     * The resource kind these cards select.
     *
     * <p>Fluid and energy cards cannot be installed together (see
     * {@link com.bobby.bobbychests.chest.menu.AbstractChestMenu#getUpgradeInstallDenyReason}),
     * but a creative-placed or command-placed pair should still resolve to something rather than
     * throw, so fluid wins the tie.
     */
    public ChestResourceMode resourceMode() {
        if (this.storesFluid) {
            return ChestResourceMode.FLUID;
        }
        if (this.storesEnergy) {
            return ChestResourceMode.ENERGY;
        }
        return ChestResourceMode.ITEM;
    }
}
