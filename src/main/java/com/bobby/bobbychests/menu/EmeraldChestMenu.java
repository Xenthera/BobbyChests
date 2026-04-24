package com.bobby.bobbychests.menu;

import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EmeraldChestMenu extends AbstractChestMenu {
    /**
     * When channel or lock/owner changes, every open emerald menu for this chest must return to row 0 or
     * EmeraldWindowSlot will read the wrong slice of the new backing storage.
     */
    public static void resetScrollForEveryoneUsingChest(ServerLevel level, BlockPos chestPos) {
        for (ServerPlayer player : level.players()) {
            if (player.level() != level) {
                continue;
            }
            if (player.containerMenu instanceof EmeraldChestMenu em && em.getChestPos().equals(chestPos)) {
                em.onGlobalStorageContextChanged();
            }
        }
    }

    private static final int SLOTS_PER_ROW = 18;
    public static final int TOTAL_CHEST_ROWS = 12;
    private static final int CHEST_ROWS_TOTAL = TOTAL_CHEST_ROWS;
    private static final int CHEST_ROWS_VISIBLE = 6;
    private static final int STORAGE_SLOTS = SLOTS_PER_ROW * CHEST_ROWS_TOTAL; // 216
    private static final int VISIBLE_CHEST_SLOTS = SLOTS_PER_ROW * CHEST_ROWS_VISIBLE; // 108

    private int scrollRows;

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
                this.addSlot(new EmeraldWindowSlot(this, this.container, row, col, x, y));
            }
        }

        int playerLeftX = 8 + ((SLOTS_PER_ROW - 9) * 18) / 2;
        this.addPlayerInventorySlots(playerInventory, playerLeftX, 140);
    }

    public int getScrollRows() {
        return this.scrollRows;
    }

    public int maxScrollRows() {
        return Math.max(0, CHEST_ROWS_TOTAL - CHEST_ROWS_VISIBLE);
    }

    /**
     * Client menus use a {@link SimpleContainer} mirror of 216 logical slots. Vanilla slot sync only writes indices
     * that appear in outgoing updates; indices that were filled while scrolled under another channel keep stale
     * stacks until cleared.
     */
    private void clearClientMirrorSlots() {
        if (!this.level.isClientSide() || !(this.container instanceof SimpleContainer mirror)) {
            return;
        }
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            mirror.setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * Call when the chest's channel, lock, or owner changes so {@link EmeraldWindowSlot} reads a different backing
     * list. Always clears scroll to row 0. On the server, always runs {@link #broadcastChanges()} even if scroll was
     * already 0 — otherwise {@code lastSlots} keeps stacks from the old channel until the menu is reopened.
     */
    public void onGlobalStorageContextChanged() {
        this.scrollRows = 0;
        this.clearClientMirrorSlots();
        if (!this.level.isClientSide()) {
            this.broadcastChanges();
        }
    }

    /** Wheel / scrollbar only. Channel or lock changes must use {@link #onGlobalStorageContextChanged()}. */
    public void setScrollRows(int rows) {
        int next = Mth.clamp(rows, 0, this.maxScrollRows());
        if (next == this.scrollRows) {
            return;
        }
        this.clearClientMirrorSlots();
        this.scrollRows = next;
        if (!this.level.isClientSide()) {
            this.broadcastChanges();
        }
    }

    /**
     * @return {@code true} if the scroll position changed
     */
    public boolean applyScrollDelta(int deltaRows) {
        int prev = this.scrollRows;
        this.setScrollRows(this.scrollRows + deltaRows);
        return this.scrollRows != prev;
    }

    @Override
    public int getImageWidthPx() {
        return 14 + (SLOTS_PER_ROW * 18);
    }

    @Override
    public int getImageHeightPx() {
        return 114 + (CHEST_ROWS_VISIBLE * 18);
    }

    private static final class EmeraldWindowSlot extends Slot {
        private final EmeraldChestMenu menu;
        private final int visibleRow;
        private final int col;

        EmeraldWindowSlot(EmeraldChestMenu menu, Container container, int visibleRow, int col, int x, int y) {
            super(container, 0, x, y);
            this.menu = menu;
            this.visibleRow = visibleRow;
            this.col = col;
        }

        private int storageIndex() {
            return (this.menu.scrollRows + this.visibleRow) * SLOTS_PER_ROW + this.col;
        }

        @Override
        public ItemStack getItem() {
            return this.container.getItem(this.storageIndex());
        }

        @Override
        public void set(ItemStack stack) {
            this.container.setItem(this.storageIndex(), stack);
            this.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.container.removeItem(this.storageIndex(), amount);
        }

        @Override
        public int getContainerSlot() {
            return this.storageIndex();
        }
    }
}
