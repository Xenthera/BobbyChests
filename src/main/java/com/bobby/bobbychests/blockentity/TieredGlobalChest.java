package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public interface TieredGlobalChest {
    ChestTier getTier();

    int getGlobalStorageId();

    void setGlobalStorageId(int globalStorageId);

    boolean isLocked();

    UUID getOwnerUuid();

    boolean canPlayerOpen(Player player);

    void setLocked(boolean locked, Player actor);
}

