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

import java.util.UUID;

public final class DirtChestMenu extends AbstractChestMenu {
    private static final int CHEST_SLOTS = 1;

    // Slot position: "5th spot in the row" (0-based col=4): x = 8 + 4*18 = 80, y = 18
    private static final int SLOT_X = 8 + 4 * 18;
    private static final int SLOT_Y = 18;

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DirtChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), BlockPos.ZERO, 0, false, null, true, ChestTier.DIRT.maxChannelId());
    }

    public static DirtChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        boolean usingGlobalStorage = buf.readBoolean();
        return new DirtChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), pos, id, locked, ownerUuid, usingGlobalStorage, maxChannelId);
    }

    public DirtChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(ModMenus.DIRT_CHEST_MENU.get(), syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);

        this.chestSlotCount = CHEST_SLOTS;
        this.addSlot(new Slot(this.container, 0, SLOT_X, SLOT_Y));

        // 1 row chest: player inventory starts at y = 18 + 18 + 14 = 50
        this.addPlayerInventorySlots(playerInventory, 8, 50);
    }

    @Override
    public int getImageWidthPx() {
        return 176;
    }

    @Override
    public int getImageHeightPx() {
        return 114 + 18; // 1 row
    }
}

