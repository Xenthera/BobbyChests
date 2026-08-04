package com.bobby.bobbychests.chest.menu.diamond;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public final class DiamondChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 18;
    private static final int ROWS = 6;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS;

    public static DiamondChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DiamondChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestTier.DIAMOND.upgradeSlotCount()),
                BlockPos.ZERO,
                0,
                false,
                null,
                true,
                ChestTier.DIAMOND.maxChannelId());
    }

    public static DiamondChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new DiamondChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                new SimpleContainer(ChestTier.DIAMOND.upgradeSlotCount()),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId());
    }

    public DiamondChestMenu(int syncID, Inventory playerInventory, Container container, Container upgradeContainer, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.DIAMOND_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

        this.addStandardChestGridSlots(SLOTS_PER_ROW, ROWS);
        this.addUpgradeSlots();
        this.addPlayerInventorySlots(
                playerInventory,
                AbstractChestMenu.playerInventoryLeftXCenteredUnderGrid(SLOTS_PER_ROW),
                AbstractChestMenu.playerInventoryTopYBelowGrid(ROWS));
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
