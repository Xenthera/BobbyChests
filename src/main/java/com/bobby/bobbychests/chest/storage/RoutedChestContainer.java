package com.bobby.bobbychests.chest.storage;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public class RoutedChestContainer implements Container {
    private final AbstractTieredChestBlockEntity chest;

    public RoutedChestContainer(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    private NonNullList<ItemStack> items() {
        return this.chest.getActiveItems();
    }

    public AbstractTieredChestBlockEntity getChest() {
        return this.chest;
    }

    @Override
    public int getContainerSize() {
        return this.chest.getSlotCount();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        // Must return the actual backing stack object. Vanilla Slot/moveItemStackTo mutates this reference.
        return this.items().get(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        NonNullList<ItemStack> items = this.items();
        ItemStack current = items.get(slot);
        if (!current.isEmpty() && this.chest.canUseDeepStorage()) {
            long deep = DeepStorageStacks.getDeepCount(current);
            if (deep > 0L) {
                int extracted = (int) Math.min((long) amount, deep);
                if (extracted <= 0) {
                    return ItemStack.EMPTY;
                }
                long next = deep - extracted;
                if (next <= 0L) {
                    items.set(slot, ItemStack.EMPTY);
                } else {
                    items.set(slot, DeepStorageStacks.makeDeepMarker(current, next));
                }
                this.chest.setChanged();
                return DeepStorageStacks.copyWithoutDeepCount(current, extracted);
            }
        }

        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            this.chest.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items().get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.items().set(slot, ItemStack.EMPTY);
        this.chest.setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> items = this.items();
        if (this.chest.canUseDeepStorage()) {
            ItemStack current = items.get(slot);
            if (stack.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
                this.chest.setChanged();
                return;
            }
            if (stack.getMaxStackSize() <= 1) {
                items.set(slot, stack);
                this.chest.setChanged();
                return;
            }
            long incomingDeep = DeepStorageStacks.getDeepCount(stack);
            if (incomingDeep > 0L) {
                if (!current.isEmpty() && !DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(current, stack)) {
                    return;
                }
                items.set(slot, DeepStorageStacks.makeDeepMarker(stack, incomingDeep));
                this.chest.setChanged();
                return;
            }
            if (!current.isEmpty()) {
                long deep = DeepStorageStacks.getDeepCount(current);
                if (deep > 0L) {
                    if (!DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(current, stack)) {
                        return;
                    }
                    long next = deep + (long) stack.getCount();
                    if (next < 0L) {
                        next = Long.MAX_VALUE;
                    }
                    items.set(slot, DeepStorageStacks.makeDeepMarker(current, next));
                    this.chest.setChanged();
                    return;
                }
                if (!DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(current, stack)) {
                    return;
                }
                items.set(slot, DeepStorageStacks.makeDeepMarker(current, (long) current.getCount() + stack.getCount()));
                this.chest.setChanged();
                return;
            }
            // Empty slot (or normal stack present): start deep storage with this stack as the marker.
            items.set(slot, DeepStorageStacks.makeDeepMarker(stack, (long) stack.getCount()));
            this.chest.setChanged();
            return;
        }

        items.set(slot, stack);
        this.chest.setChanged();
    }

    @Override
    public void setChanged() {
        this.chest.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!this.chest.canPlayerOpen(player)) {
            return false;
        }
        return Container.stillValidBlockEntity(this.chest, player);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (this.chest.getStorageMode() == ChestStorageMode.GLOBAL && this.chest.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).onChestOpen(this.chest);
        }
        this.chest.startOpen(user);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (this.chest.getStorageMode() == ChestStorageMode.GLOBAL && this.chest.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).onChestClose(this.chest);
        }
        this.chest.stopOpen(user);
    }

    @Override
    public void clearContent() {
        if (this.chest.getStorageMode() != ChestStorageMode.GLOBAL) {
            NonNullList<ItemStack> items = this.items();
            for (int slot = 0; slot < items.size(); slot++) {
                items.set(slot, ItemStack.EMPTY);
            }
            this.chest.setChanged();
        }
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return this.items().iterator();
    }
}
