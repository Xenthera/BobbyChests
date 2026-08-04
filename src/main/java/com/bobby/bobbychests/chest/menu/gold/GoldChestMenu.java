package com.bobby.bobbychests.chest.menu.gold;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import com.bobby.bobbychests.chest.ChestTier;

import java.util.UUID;

public final class GoldChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 6;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS;

    public static GoldChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new GoldChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), new SimpleContainer(ChestTier.GOLD.upgradeSlotCount()), BlockPos.ZERO, 0, false, null, true, ChestTier.GOLD.maxChannelId());
    }

    public static GoldChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new GoldChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestTier.GOLD.upgradeSlotCount()),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId());
    }

    public GoldChestMenu(int syncID, Inventory playerInventory, Container container, Container upgradeContainer, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.GOLD_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

        this.addStandardChestGridSlots(SLOTS_PER_ROW, ROWS);
        this.addUpgradeSlots();
        this.addPlayerInventorySlots(playerInventory, 8, AbstractChestMenu.playerInventoryTopYBelowGrid(ROWS));
    }

    @Override
    public int getImageWidthPx() {
        return AbstractChestMenu.imageWidthChestGridPlusUpgradeStrip(SLOTS_PER_ROW);
    }

    @Override
    public int getImageHeightPx() {
        return AbstractChestMenu.imageHeightForChestRows(ROWS);
    }
}
