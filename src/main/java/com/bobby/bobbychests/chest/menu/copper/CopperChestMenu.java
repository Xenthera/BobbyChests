package com.bobby.bobbychests.chest.menu.copper;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import com.bobby.bobbychests.chest.ChestTier;

import java.util.UUID;

public final class CopperChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 2;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS;

    public static CopperChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new CopperChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), new SimpleContainer(ChestTier.COPPER.upgradeSlotCount()), BlockPos.ZERO, 0, false, null, true, ChestTier.COPPER.maxChannelId());
    }

    public static CopperChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new CopperChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestTier.COPPER.upgradeSlotCount()),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId());
    }

    public CopperChestMenu(int syncID, Inventory playerInventory, Container container, Container upgradeContainer, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.COPPER_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

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
