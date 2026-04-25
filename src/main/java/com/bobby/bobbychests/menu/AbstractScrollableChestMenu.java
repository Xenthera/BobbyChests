package com.bobby.bobbychests.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Chest menu with a vertical scroll offset mapping visible slots into a larger logical getContainer().
 */
public abstract class AbstractScrollableChestMenu extends AbstractChestMenu {
    /**
     * When channel or lock/owner changes, every open scrollable menu for this chest must return to row 0 or window
     * slots will read the wrong slice of the new backing storage.
     */
    public static void resetScrollForEveryoneUsingChest(ServerLevel level, BlockPos chestPos) {
        for (ServerPlayer player : level.players()) {
            if (player.level() != level) {
                continue;
            }
            if (player.containerMenu instanceof AbstractScrollableChestMenu m && m.getChestPos().equals(chestPos)) {
                m.onGlobalStorageContextChanged();
            }
        }
    }

    private int scrollRows;

    protected AbstractScrollableChestMenu(
            MenuType<?> type,
            int syncID,
            Inventory playerInventory,
            Container container,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            int maxChannelId) {
        super(type, syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, maxChannelId);
    }

    protected abstract int slotsPerRow();

    protected abstract int chestRowsTotal();

    protected abstract int chestRowsVisible();

    protected final int logicalStorageSlotCount() {
        return this.slotsPerRow() * this.chestRowsTotal();
    }

    public final int getScrollRows() {
        return this.scrollRows;
    }

    public int maxScrollRows() {
        return Math.max(0, this.chestRowsTotal() - this.chestRowsVisible());
    }

    /**
     * Client menus use a SimpleContainer mirror of all logical slots. Vanilla slot sync only writes indices
     * that appear in outgoing updates; indices that were filled while scrolled under another channel keep stale stacks
     * until cleared.
     */
    private void clearClientMirrorSlots() {
        if (!this.level.isClientSide() || !(this.container instanceof SimpleContainer mirror)) {
            return;
        }
        int n = this.logicalStorageSlotCount();
        for (int i = 0; i < n; i++) {
            mirror.setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * Call when the chest's channel, lock, or owner changes so window slots read a different backing list. Always
     * clears scroll to row 0. On the server, always runs broadcastChanges() even if scroll was already 0 —
     * otherwise lastSlots keeps stacks from the old channel until the menu is reopened.
     */
    public void onGlobalStorageContextChanged() {
        this.scrollRows = 0;
        this.clearClientMirrorSlots();
        if (!this.level.isClientSide()) {
            this.broadcastChanges();
        }
    }

    /** Wheel / scrollbar only. Channel or lock changes must use onGlobalStorageContextChanged(). */
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
     * @return true if the scroll position changed
     */
    public boolean applyScrollDelta(int deltaRows) {
        int prev = this.scrollRows;
        this.setScrollRows(this.scrollRows + deltaRows);
        return this.scrollRows != prev;
    }

    public static final class ScrollWindowSlot extends Slot {
        private final AbstractScrollableChestMenu menu;
        private final int visibleRow;
        private final int col;

        ScrollWindowSlot(AbstractScrollableChestMenu menu, Container container, int visibleRow, int col, int x, int y) {
            super(container, 0, x, y);
            this.menu = menu;
            this.visibleRow = visibleRow;
            this.col = col;
        }

        private int storageIndex() {
            return (this.menu.getScrollRows() + this.visibleRow) * this.menu.slotsPerRow() + this.col;
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
