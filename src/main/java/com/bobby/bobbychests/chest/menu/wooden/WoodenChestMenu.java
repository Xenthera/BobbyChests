package com.bobby.bobbychests.chest.menu.wooden;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import com.bobby.bobbychests.chest.ChestTier;

import java.util.UUID;

public final class WoodenChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 1;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS;

    public static WoodenChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new WoodenChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), new SimpleContainer(ChestUpgradeManager.SLOT_COUNT), BlockPos.ZERO, 0, false, null, true, ChestTier.WOOD.maxChannelId());
    }

    public static WoodenChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new WoodenChestMenu(
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

    public WoodenChestMenu(int syncID, Inventory playerInventory, Container container, Container upgradeContainer, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.WOODEN_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

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
