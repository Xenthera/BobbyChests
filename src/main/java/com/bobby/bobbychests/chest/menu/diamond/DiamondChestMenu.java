package com.bobby.bobbychests.chest.menu.diamond;

import com.bobby.bobbychests.chest.ChestTier;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

/**
 * Same panel width as gold (9 columns, 6 visible rows) with scroll for the full 108 slots.
 */
public final class DiamondChestMenu extends AbstractScrollableChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    public static final int TOTAL_CHEST_ROWS = 12;
    private static final int CHEST_ROWS_VISIBLE = 6;
    private static final int STORAGE_SLOTS = SLOTS_PER_ROW * TOTAL_CHEST_ROWS;

    public static DiamondChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DiamondChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(STORAGE_SLOTS),
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
                new SimpleContainer(STORAGE_SLOTS),
                new SimpleContainer(ChestTier.DIAMOND.upgradeSlotCount()),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId());
    }

    public DiamondChestMenu(
            int syncID,
            Inventory playerInventory,
            Container container,
            Container upgradeContainer,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId) {
        super(
                ModMenus.DIAMOND_CHEST_MENU.get(),
                syncID,
                playerInventory,
                container,
                upgradeContainer,
                chestPos,
                initialChestId,
                initialLocked,
                initialOwnerUuid,
                initialUsingGlobalStorage,
                maxChannelId,
                SLOTS_PER_ROW,
                TOTAL_CHEST_ROWS,
                CHEST_ROWS_VISIBLE);
        this.addScrollableChestSlots(playerInventory);
    }

    @Override
    public int getImageWidthPx() {
        return AbstractChestMenu.scrollableChestPanelWidthPx(SLOTS_PER_ROW);
    }

    @Override
    public int getImageHeightPx() {
        return AbstractChestMenu.imageHeightForChestRows(CHEST_ROWS_VISIBLE);
    }
}
