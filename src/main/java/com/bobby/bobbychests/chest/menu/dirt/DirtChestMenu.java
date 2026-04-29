package com.bobby.bobbychests.chest.menu.dirt;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.bobby.bobbychests.chest.ChestTier;

import java.util.UUID;

public final class DirtChestMenu extends AbstractChestMenu {
    private static final int CHEST_SLOTS = 1;
    private static final int SLOTS_PER_ROW_FOR_SIZE = 9;
    private static final int ROWS_FOR_SIZE = 1;
    private static final int SLOT_X = 8 + 4 * 18;
    private static final int SLOT_Y = 18;

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DirtChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), new SimpleContainer(ChestUpgradeManager.SLOT_COUNT), BlockPos.ZERO, 0, false, null, true, ChestTier.DIRT.maxChannelId());
    }

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new DirtChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestUpgradeManager.SLOT_COUNT),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId());
    }

    public DirtChestMenu(int syncID, Inventory playerInventory, Container container, Container upgradeContainer, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.DIRT_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

        this.chestSlotCount = CHEST_SLOTS;
        this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y));

        this.addUpgradeSlots();

        this.addPlayerInventorySlots(playerInventory, 8, AbstractChestMenu.playerInventoryTopYBelowGrid(ROWS_FOR_SIZE));
    }

    @Override
    public int getImageWidthPx() {
        return AbstractChestMenu.imageWidthChestGridPlusUpgradeStrip(SLOTS_PER_ROW_FOR_SIZE);
    }

    @Override
    public int getImageHeightPx() {
        return AbstractChestMenu.imageHeightForChestRows(ROWS_FOR_SIZE);
    }
}
