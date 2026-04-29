package com.bobby.bobbychests.chest.upgrade;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public final class ChestUpgradeContainer implements Container {
    private final ChestUpgradeManager manager;

    public ChestUpgradeContainer(AbstractTieredChestBlockEntity chest) {
        this.manager = chest.getUpgradeManager();
    }

    public ChestUpgradeManager getManager() {
        return this.manager;
    }

    private NonNullList<ItemStack> stacks() {
        return this.manager.getStacks();
    }

    @Override
    public int getContainerSize() {
        return this.manager.getSlotCount();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.stacks()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.stacks().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.stacks(), slot, amount);
        if (!result.isEmpty()) {
            this.manager.onContentsChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.stacks().get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.stacks().set(slot, ItemStack.EMPTY);
        this.manager.onContentsChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stacks().set(slot, stack);
        this.manager.onContentsChanged();
    }

    @Override
    public void setChanged() {
        this.manager.onContentsChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.manager.stillValid(player);
    }

    @Override
    public void startOpen(ContainerUser user) {
    }

    @Override
    public void stopOpen(ContainerUser user) {
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.stacks().set(i, ItemStack.EMPTY);
        }
        this.manager.onContentsChanged();
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return this.stacks().iterator();
    }
}
