package com.bobby.bobbychests.chest.storage;

/**
 * What kind of resource a chest stores.
 *
 * <p>Orthogonal to {@link ChestStorageMode}: any mode can be LOCAL or GLOBAL, and a global
 * pool is keyed per resource kind, so channel 5 items, channel 5 fluid, and channel 5 energy
 * are three unrelated storages.
 *
 * <p>Derived from the installed upgrade cards rather than stored as an independent setting.
 * See {@link com.bobby.bobbychests.chest.upgrade.ChestUpgradeCapabilities}.
 */
public enum ChestResourceMode {
    ITEM("item"),
    FLUID("fluid"),
    ENERGY("energy");

    private final String id;

    ChestResourceMode(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public boolean isItem() {
        return this == ITEM;
    }

    public static ChestResourceMode fromIdOrDefault(String id, ChestResourceMode defaultMode) {
        if (id == null || id.isBlank()) {
            return defaultMode;
        }
        for (ChestResourceMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return defaultMode;
    }
}
