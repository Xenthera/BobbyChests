package com.bobby.bobbychests.chest.menu;

import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;
import com.bobby.bobbychests.network.SetScrollableChestScrollPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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
                PacketDistributor.sendToPlayer(player, new SetScrollableChestScrollPayload(chestPos, 0));
                m.onGlobalStorageContextChanged();
            }
        }
    }

    private int scrollRows;
    private boolean pendingFullStateBroadcast;
    private final int slotsPerRow;
    private final int chestRowsTotal;
    private final int chestRowsVisible;

    protected AbstractScrollableChestMenu(
            MenuType<?> type,
            int syncID,
            Inventory playerInventory,
            Container container,
            Container upgradeContainer,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId,
            int slotsPerRow,
            int chestRowsTotal,
            int chestRowsVisible) {
        super(type, syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);
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

        this.addUpgradeSlots();

        int playerLeftX = this.chestSlotGridLeft() + ((this.slotsPerRow() - 9) * step) / 2;
        this.addPlayerInventorySlots(playerInventory, playerLeftX, this.playerInventoryTopY());
    }

    /**
     * When storage identity changes (channel, lock/global-pool swap, networked storage mode flip), both sides must snap
     * before slot packets are applied. {@link ScrollWindowSlot#set} writes into the logical mirror using
     * {@link #scrollRows}; if the client still has the old offset, freshly-synced row-zero stacks are stored into the
     * wrong slice and disappear once the viewport snaps later.
     */
    public void onGlobalStorageContextChanged() {
        if (this.level.isClientSide()) {
            this.scrollRows = 0;
            return;
        }
        this.scrollRows = 0;
        this.queueFullStateBroadcast();
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

    private void queueFullStateBroadcast() {
        if (!this.level.isClientSide()) {
            this.pendingFullStateBroadcast = true;
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!this.pendingFullStateBroadcast) {
            return;
        }
        this.pendingFullStateBroadcast = false;
        this.broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack previous = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return previous;
        }

        ItemStack stack = slot.getItem();
        previous = stack.copy();
        if (index < this.chestSlotCount) {
            if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < this.getFirstPlayerSlotIndex()) {
            if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (ChestUpgradeManager.isUpgradeCard(stack)) {
                if (!this.moveItemStackTo(stack, this.getFirstUpgradeSlotIndex(), this.getFirstPlayerSlotIndex(), false)
                        && !this.moveItemStackToLogicalChest(stack)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackToLogicalChest(stack)) {
                if (this.hasVoidUpgradeInstalled()) {
                    stack.setCount(0);
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return previous;
    }

    private boolean moveItemStackToLogicalChest(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean changed = false;
        int size = Math.min(this.logicalStorageSlotCount(), this.container.getContainerSize());

        if (stack.isStackable()) {
            for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
                ItemStack existing = this.container.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                    continue;
                }
                int max = Math.min(existing.getMaxStackSize(), this.container.getMaxStackSize());
                int room = max - existing.getCount();
                if (room <= 0) {
                    continue;
                }
                int move = Math.min(room, stack.getCount());
                existing.grow(move);
                stack.shrink(move);
                this.container.setChanged();
                changed = true;
            }
        }

        for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
            if (!this.container.getItem(slot).isEmpty() || !this.container.canPlaceItem(slot, stack)) {
                continue;
            }
            int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), this.container.getMaxStackSize()));
            this.container.setItem(slot, stack.copyWithCount(move));
            stack.shrink(move);
            changed = true;
        }

        return changed;
    }

    /** @return {@code true} if the scroll position changed */
    public boolean applyScrollDelta(int deltaRows) {
        int prev = this.scrollRows;
        this.setScrollRows(this.scrollRows + deltaRows);
        return this.scrollRows != prev;
    }
}
