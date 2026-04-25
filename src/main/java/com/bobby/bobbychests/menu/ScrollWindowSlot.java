package com.bobby.bobbychests.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Maps visible chest slots into a larger logical {@link Container} using {@link AbstractScrollableChestMenu#getScrollRows()}.
 */
public final class ScrollWindowSlot extends Slot {
    private final AbstractScrollableChestMenu menu;
    private final int visibleRow;
    private final int col;

    public ScrollWindowSlot(AbstractScrollableChestMenu menu, Container container, int visibleRow, int col, int x, int y) {
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
