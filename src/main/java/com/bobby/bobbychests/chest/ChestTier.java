package com.bobby.bobbychests.chest;

public enum ChestTier {
    // "Channel" cap per tier. Dirt stays tiny; higher tiers scale up.
    // UI supports up to 999,999,999 so future Netherite can live there.
    DIRT("dirt", 3, 1, 1),
    WOOD("wood", 9, 1, 9),
    COPPER("copper", 99, 2, 18),
    IRON("iron", 999, 2, 27),
    GOLD("gold", 9_999, 3, 54),
    DIAMOND("diamond", 99_999, 4, 9 * 12),
    EMERALD("emerald", 999_999, 4, 9 * 24),
    NETHERITE("netherite", 9_999_999, 4, 9 * 36);

    public static final int MAX_UPGRADE_SLOTS = 4;

    /** Tank size per item slot the tier would otherwise offer, in millibuckets. */
    private static final int FLUID_CAPACITY_PER_SLOT_MB = 8_000;
    /** FE buffer size per item slot the tier would otherwise offer. */
    private static final int ENERGY_CAPACITY_PER_SLOT_FE = 100_000;

    private final String id;
    private final int maxChannelId;
    private final int upgradeSlotCount;
    private final int storageSlots;

    ChestTier(String id, int maxChannelId, int upgradeSlotCount, int storageSlots) {
        this.id = id;
        this.maxChannelId = maxChannelId;
        this.upgradeSlotCount = upgradeSlotCount;
        this.storageSlots = storageSlots;
    }

    /** Item slots this tier stores. Also the basis for its fluid and energy capacity. */
    public int storageSlots() {
        return this.storageSlots;
    }

    /**
     * Tank capacity in millibuckets, from dirt's 8k up to netherite's ~2.6M.
     *
     * <p>Derived from {@link #storageSlots()} so the two progressions cannot drift apart, and
     * comfortably int-sized: the deep storage card, which is what would otherwise multiply this,
     * is refused in fluid mode.
     */
    public int fluidCapacityMb() {
        return this.storageSlots * FLUID_CAPACITY_PER_SLOT_MB;
    }

    /** FE buffer capacity, scaled off {@link #storageSlots()} exactly as {@link #fluidCapacityMb()} is. */
    public int energyCapacityFe() {
        return this.storageSlots * ENERGY_CAPACITY_PER_SLOT_FE;
    }

    public String id() {
        return this.id;
    }

    public int maxChannelId() {
        return this.maxChannelId;
    }

    public int upgradeSlotCount() {
        return this.upgradeSlotCount;
    }

    public static ChestTier fromIdOrDefault(String id, ChestTier defaultTier) {
        if (id == null || id.isBlank()) {
            return defaultTier;
        }
        for (ChestTier tier : values()) {
            if (tier.id.equalsIgnoreCase(id)) {
                return tier;
            }
        }
        return defaultTier;
    }
}
