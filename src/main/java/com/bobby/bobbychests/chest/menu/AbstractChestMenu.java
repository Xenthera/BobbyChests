package com.bobby.bobbychests.chest.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared plumbing for global chest menus (lock/id metadata + quick-move).
 * Concrete tiers control slot layout and GUI dimensions.
 */
public abstract class AbstractChestMenu extends AbstractContainerMenu {
    protected final Container container;
    protected final Level level;
    protected final BlockPos chestPos;
    protected final int initialChestId;
    protected final boolean initialLocked;
    protected final UUID initialOwnerUuid;
    protected final int maxChannelId;

    protected int chestSlotCount;

    protected AbstractChestMenu(net.minecraft.world.inventory.MenuType<?> type, int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid, int maxChannelId) {
        super(type, syncID);
        this.container = Objects.requireNonNull(container);
        this.level = playerInventory.player.level();
        this.chestPos = chestPos;
        this.initialChestId = initialChestId;
        this.initialLocked = initialLocked;
        this.initialOwnerUuid = initialOwnerUuid;
        this.maxChannelId = maxChannelId;

        this.container.startOpen(playerInventory.player);
    }

    public abstract int getImageWidthPx();

    public abstract int getImageHeightPx();

    public final Container getContainer() {
        return this.container;
    }

    public final BlockPos getChestPos() {
        return this.chestPos;
    }

    public final int getInitialChestId() {
        return this.initialChestId;
    }

    public final int getMaxChannelId() {
        return this.maxChannelId;
    }

    public final boolean getInitialLocked() {
        return this.initialLocked;
    }

    public final UUID getInitialOwnerUuid() {
        return this.initialOwnerUuid;
    }

    public final int getChestSlotCount() {
        return this.chestSlotCount;
    }

    protected final void addPlayerInventorySlots(Inventory playerInventory, int leftX, int topY) {
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftX + col * 18, topY + row * 18));
            }
        }
        // Hotbar
        int hotbarY = topY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftX + col * 18, hotbarY));
        }
    }

    /**
     * Chest slots must be added before this is called, and {@link #chestSlotCount} must be set.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack previous = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            previous = stack.copy();
            if (index < this.chestSlotCount) {
                if (!this.moveItemStackTo(stack, this.chestSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return previous;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public Level getLevel() {
        return this.level;
    }
}

