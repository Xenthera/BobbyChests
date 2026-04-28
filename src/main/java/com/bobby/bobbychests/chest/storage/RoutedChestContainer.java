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
        return this.items().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items(), slot, amount);
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
        this.items().set(slot, stack);
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
