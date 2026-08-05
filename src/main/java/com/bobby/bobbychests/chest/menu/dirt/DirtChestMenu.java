package com.bobby.bobbychests.chest.menu.dirt;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.bobby.bobbychests.chest.ChestTier;
import com.bobby.bobbycore.client.gui.layout.GuiLayout;

import java.util.UUID;

public final class DirtChestMenu extends AbstractChestMenu {
    private static final int CHEST_SLOTS = 1;
    private static final int SLOTS_PER_ROW_FOR_SIZE = 9;
    private static final int ROWS_FOR_SIZE = 1;
    private static final int SLOT_X = GuiLayout.contentSlotOriginX() + 4 * 18;
    private static final int SLOT_Y = com.bobby.bobbycore.client.gui.layout.GuiLayout.slottedContentTop();

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DirtChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), new SimpleContainer(ChestTier.DIRT.upgradeSlotCount()), BlockPos.ZERO, 0, false, null, true, ChestTier.DIRT.maxChannelId());
    }

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new DirtChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestTier.DIRT.upgradeSlotCount()),
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
        this.setChestGridSize(1, 1);
        this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y));

        this.addUpgradeSlots();

        this.addPlayerInventorySlots(
                playerInventory,
                AbstractChestMenu.playerInventoryLeftXCenteredUnderGrid(SLOTS_PER_ROW_FOR_SIZE),
                AbstractChestMenu.playerInventoryTopYBelowGrid(ROWS_FOR_SIZE));
    }

    @Override
    public int chestSlotGridLeft() {
        return SLOT_X;
    }

    @Override
    protected boolean allowDeepStorageUpgradeCard() {
        return true;
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
