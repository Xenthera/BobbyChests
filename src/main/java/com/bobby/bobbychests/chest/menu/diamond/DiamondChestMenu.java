package com.bobby.bobbychests.chest.menu.diamond;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.UUID;

public final class DiamondChestMenu extends AbstractChestMenu {
    private static final int SLOTS_PER_ROW = 18;
    private static final int ROWS = 6;
    private static final int CHEST_SLOTS = SLOTS_PER_ROW * ROWS; // 108

    public static DiamondChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new DiamondChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(CHEST_SLOTS),
                BlockPos.ZERO,
                0,
                false,
                null,
                ChestTier.DIAMOND.maxChannelId()
        );
    }

    public static DiamondChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        return new DiamondChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), pos, id, locked, ownerUuid, maxChannelId);
    }

    public DiamondChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, int maxChannelId) {
        super(ModMenus.DIAMOND_CHEST_MENU.get(), syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, maxChannelId);

        // Chest slots (18x6), pushed into the top-left of the texture.
        this.chestSlotCount = CHEST_SLOTS;
        int slotIndex = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                this.addSlot(new Slot(this.container, slotIndex++, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory/hotbar: keep the same Y as a 6-row grid, but center under the 18-wide grid.
        int playerLeftX = 8 + ((SLOTS_PER_ROW - 9) * 18) / 2; // center 9-wide player inv under 18-wide chest
        this.addPlayerInventorySlots(playerInventory, playerLeftX, 140);
    }

    @Override
    public int getImageWidthPx() {
        // GUI logical width; texture itself is 512x512.
        // Matches vanilla formula: 14 + columns*18.
        return 14 + (SLOTS_PER_ROW * 18); // 338
    }

    @Override
    public int getImageHeightPx() {
        // Same height as a 6-row chest UI (even though the texture is 512x512).
        return 114 + (ROWS * 18); // 222
    }
}

