package com.bobby.bobbychests.chest.upgrade;

/**
 * Feature flags derived from installed upgrade cards ({@link ChestUpgradeManager#capabilities()}).
 *
 * @param canUseGlobalPooledStorage true when a networking upgrade is present in any upgrade slot
 */
public record ChestUpgradeCapabilities(boolean canUseGlobalPooledStorage) {}
