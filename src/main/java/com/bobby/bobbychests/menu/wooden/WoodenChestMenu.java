package com.bobby.bobbychests.menu.wooden;

import com.bobby.bobbychests.menu.ModMenus;
import com.bobby.bobbychests.menu.AbstractChestMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.bobby.bobbychests.tier.ChestTier;

import java.util.UUID;

public final class WoodenChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 1;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS; // 9

    public static WoodenChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new WoodenChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), BlockPos.ZERO, 0, false, null, ChestTier.WOOD.maxChannelId());
    }

    public static WoodenChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        return new WoodenChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), pos, id, locked, ownerUuid, maxChannelId);
    }

    public WoodenChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, int maxChannelId) {
        super(ModMenus.WOODEN_CHEST_MENU.get(), syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, maxChannelId);

        // Chest slots (9x1)
        this.chestSlotCount = CHEST_SLOTS;
        int slotIndex = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                this.addSlot(new Slot(this.container, slotIndex++, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory below
        this.addPlayerInventorySlots(playerInventory, 8, 50);
    }

    @Override
    public int getImageWidthPx() {
        return 176;
    }

    @Override
    public int getImageHeightPx() {
        // matches existing texture layout
        return 114 + ROWS * 18;
    }
}

