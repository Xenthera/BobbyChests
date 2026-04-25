package com.bobby.bobbychests.tier;

public enum ChestTier {
    // "Channel" cap per tier. Dirt stays tiny; higher tiers scale up.
    // UI supports up to 999,999,999 so future Netherite can live there.
    DIRT("dirt", 3),
    WOOD("wood", 9),
    COPPER("copper", 99),
    IRON("iron", 999),
    GOLD("gold", 9_999),
    DIAMOND("diamond", 99_999),
    EMERALD("emerald", 999_999),
    NETHERITE("netherite", 9_999_999);

    private final String id;
    private final int maxChannelId;

    ChestTier(String id, int maxChannelId) {
        this.id = id;
        this.maxChannelId = maxChannelId;
    }

    public String id() {
        return this.id;
    }

    public int maxChannelId() {
        return this.maxChannelId;
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

