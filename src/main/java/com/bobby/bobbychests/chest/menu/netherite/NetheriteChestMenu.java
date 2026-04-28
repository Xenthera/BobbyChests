package com.bobby.bobbychests.chest.menu.netherite;

import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public final class NetheriteChestMenu extends AbstractScrollableChestMenu {
    private static final int SLOTS_PER_ROW = 18;
    /** Logical grid height in rows (18×18 = 324 storage slots). */
    public static final int TOTAL_CHEST_ROWS = 18;
    private static final int CHEST_ROWS_VISIBLE = 6;
    private static final int STORAGE_SLOTS = SLOTS_PER_ROW * TOTAL_CHEST_ROWS;

    public static NetheriteChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new NetheriteChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(STORAGE_SLOTS),
                BlockPos.ZERO,
                0,
                false,
                null,
                true,
                ChestTier.NETHERITE.maxChannelId()
        );
    }

    public static NetheriteChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int maxChannelId = buf.readVarInt();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        boolean usingGlobalStorage = buf.readBoolean();
        return new NetheriteChestMenu(syncId, playerInventory, new SimpleContainer(STORAGE_SLOTS), pos, id, locked, ownerUuid, usingGlobalStorage, maxChannelId);
    }

    public NetheriteChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, boolean initialUsingGlobalStorage, int maxChannelId) {
        super(
                ModMenus.NETHERITE_CHEST_MENU.get(),
                syncID,
                playerInventory,
                container,
                chestPos,
                initialChestId,
                initialLocked,
                initialOwnerUuid,
                initialUsingGlobalStorage,
                maxChannelId,
                SLOTS_PER_ROW,
                TOTAL_CHEST_ROWS,
                CHEST_ROWS_VISIBLE
        );
        this.addScrollableChestSlots(playerInventory);
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
