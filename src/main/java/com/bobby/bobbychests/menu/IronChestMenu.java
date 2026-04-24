package com.bobby.bobbychests.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.bobby.bobbychests.tier.ChestTier;

import java.util.UUID;

public final class IronChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 3;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS; // 27

    public static IronChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new IronChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), BlockPos.ZERO, 0, false, null, ChestTier.IRON.maxChannelId());
    }

    public static IronChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        return new IronChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), pos, id, locked, ownerUuid, maxChannelId);
    }

    public IronChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, int maxChannelId) {
        super(ModMenus.IRON_CHEST_MENU.get(), syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, maxChannelId);

        this.chestSlotCount = CHEST_SLOTS;
        int slotIndex = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                this.addSlot(new Slot(this.container, slotIndex++, 8 + col * 18, 18 + row * 18));
            }
        }

        this.addPlayerInventorySlots(playerInventory, 8, 86);
    }

    @Override
    public int getImageWidthPx() {
        return 176;
    }

    @Override
    public int getImageHeightPx() {
        return 114 + ROWS * 18;
    }
}

