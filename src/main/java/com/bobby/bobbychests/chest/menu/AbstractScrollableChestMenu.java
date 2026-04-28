package com.bobby.bobbychests.chest.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Scrollable chest menu with a client-side row offset and server full-state sync after the visible window changes.
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
    private final int slotsPerRow;
    private final int chestRowsTotal;
    private final int chestRowsVisible;

    protected AbstractScrollableChestMenu(
            MenuType<?> type,
            int syncID,
            Inventory playerInventory,
            Container container,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId,
            int slotsPerRow,
            int chestRowsTotal,
            int chestRowsVisible) {
        super(type, syncID, playerInventory, container, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);
        this.slotsPerRow = slotsPerRow;
        this.chestRowsTotal = chestRowsTotal;
        this.chestRowsVisible = chestRowsVisible;
    }

    protected final int slotsPerRow() {
        return this.slotsPerRow;
    }

    protected final int chestRowsTotal() {
        return this.chestRowsTotal;
    }

    protected final int chestRowsVisible() {
        return this.chestRowsVisible;
    }

    /** Used by scrollable chest screens. */
    public final int getTotalChestRows() {
        return this.chestRowsTotal();
    }

    public final int getVisibleChestRows() {
        return this.chestRowsVisible();
    }

    public final int getSlotsPerRow() {
        return this.slotsPerRow();
    }

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
     * Chest grid origin and step in GUI pixels — must match {@link ScrollWindowSlot} x/y from the tier menu.
     * {@link com.bobby.bobbychests.client.chest.screen.AbstractScrollableChestScreen} uses these for hit-testing.
     */
    public int chestSlotGridLeft() {
        return 8;
    }

    public int chestSlotGridTop() {
        return 18;
    }

    public int chestSlotStep() {
        return 18;
    }

    protected int playerInventoryTopY() {
        return this.chestSlotGridTop() + this.chestRowsVisible() * this.chestSlotStep() + 14;
    }

    protected final void addScrollableChestSlots(Inventory playerInventory) {
        int step = this.chestSlotStep();
        this.chestSlotCount = this.slotsPerRow() * this.chestRowsVisible();

        for (int row = 0; row < this.chestRowsVisible(); row++) {
            for (int col = 0; col < this.slotsPerRow(); col++) {
                int x = this.chestSlotGridLeft() + col * step;
                int y = this.chestSlotGridTop() + row * step;
                this.addSlot(new ScrollWindowSlot(this, this.container, row, col, x, y));
            }
        }

        int playerLeftX = this.chestSlotGridLeft() + ((this.slotsPerRow() - 9) * step) / 2;
        this.addPlayerInventorySlots(playerInventory, playerLeftX, this.playerInventoryTopY());
    }

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
     * When storage identity changes, reset scroll to 0 and clear the client mirror. Server resyncs fully so
     * {@code lastSlots} does not keep stacks from the old channel.
     */
    public void onGlobalStorageContextChanged() {
        this.scrollRows = 0;
        this.clearClientMirrorSlots();
        if (!this.level.isClientSide()) {
            this.broadcastFullState();
        }
    }

    /**
     * Wheel / scrollbar on the client, or scroll application from {@code SetScrollableChestScrollPayload} on the server.
     * The client mirror is preserved during scroll so already-synced rows do not flash empty while the full server
     * state arrives.
     */
    public void setScrollRows(int rows) {
        int next = Mth.clamp(rows, 0, this.maxScrollRows());
        if (next == this.scrollRows) {
            return;
        }
        this.scrollRows = next;
        if (!this.level.isClientSide()) {
            // Visible menu slot IDs now point at different logical storage slots. A normal incremental broadcast
            // compares by visible slot ID and can skip equal-looking stacks, leaving the client mirror incomplete.
            this.broadcastFullState();
        }
    }

    /** @return {@code true} if the scroll position changed */
    public boolean applyScrollDelta(int deltaRows) {
        int prev = this.scrollRows;
        this.setScrollRows(this.scrollRows + deltaRows);
        return this.scrollRows != prev;
    }
}
