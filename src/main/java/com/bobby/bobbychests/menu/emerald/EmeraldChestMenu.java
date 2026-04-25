package com.bobby.bobbychests.menu.emerald;

import com.bobby.bobbychests.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.menu.ModMenus;
import com.bobby.bobbychests.menu.ScrollWindowSlot;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public final class EmeraldChestMenu extends AbstractScrollableChestMenu {
    private static final int SLOTS_PER_ROW = 18;
    public static final int TOTAL_CHEST_ROWS = 12;
    private static final int CHEST_ROWS_TOTAL = TOTAL_CHEST_ROWS;
    private static final int CHEST_ROWS_VISIBLE = 6;
    private static final int STORAGE_SLOTS = SLOTS_PER_ROW * CHEST_ROWS_TOTAL;
    private static final int VISIBLE_CHEST_SLOTS = SLOTS_PER_ROW * CHEST_ROWS_VISIBLE;

    public static EmeraldChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new EmeraldChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(STORAGE_SLOTS),
                BlockPos.ZERO,
                0,
                false,
                null,
                ChestTier.EMERALD.maxChannelId()
        );
    }

    public static EmeraldChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        return new EmeraldChestMenu(syncId, playerInventory, new SimpleContainer(STORAGE_SLOTS), pos, id, locked, ownerUuid, maxChannelId);
    }

    public EmeraldChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, int maxChannelId) {
        super(ModMenus.EMERALD_CHEST_MENU.get(), syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, maxChannelId);

        this.chestSlotCount = VISIBLE_CHEST_SLOTS;
        for (int row = 0; row < CHEST_ROWS_VISIBLE; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new ScrollWindowSlot(this, this.container, row, col, x, y));
            }
        }

        int playerLeftX = 8 + ((SLOTS_PER_ROW - 9) * 18) / 2;
        this.addPlayerInventorySlots(playerInventory, playerLeftX, 140);
    }

    @Override
    protected int slotsPerRow() {
        return SLOTS_PER_ROW;
    }

    @Override
    protected int chestRowsTotal() {
        return CHEST_ROWS_TOTAL;
    }

    @Override
    protected int chestRowsVisible() {
        return CHEST_ROWS_VISIBLE;
    }

    @Override
    public int getImageWidthPx() {
        return 14 + (SLOTS_PER_ROW * 18);
    }

    @Override
    public int getImageHeightPx() {
        return 114 + (CHEST_ROWS_VISIBLE * 18);
    }
}
