package com.bobby.bobbychests.chest;

public enum ChestTier {
    // "Channel" cap per tier. Dirt stays tiny; higher tiers scale up.
    // UI supports up to 999,999,999 so future Netherite can live there.
    DIRT("dirt", 3, 1),
    WOOD("wood", 9, 1),
    COPPER("copper", 99, 2),
    IRON("iron", 999, 2),
    GOLD("gold", 9_999, 3),
    DIAMOND("diamond", 99_999, 4),
    EMERALD("emerald", 999_999, 4),
    NETHERITE("netherite", 9_999_999, 4);

    public static final int MAX_UPGRADE_SLOTS = 4;

    private final String id;
    private final int maxChannelId;
    private final int upgradeSlotCount;

    ChestTier(String id, int maxChannelId, int upgradeSlotCount) {
        this.id = id;
        this.maxChannelId = maxChannelId;
        this.upgradeSlotCount = upgradeSlotCount;
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
