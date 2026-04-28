package com.bobby.bobbychests.chest.storage;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public class GlobalTieredChestContainer implements Container {

    private final GlobalTieredChestData data;
    private final AbstractTieredChestBlockEntity chest;

    public GlobalTieredChestContainer(GlobalTieredChestData data, AbstractTieredChestBlockEntity chest) {
        this.data = data;
        this.chest = chest;
    }

    private NonNullList<ItemStack> items() {
        return this.data.getItemsForChest(this.chest);
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
            if (!stack.isEmpty()) return false;
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
        if (!result.isEmpty()) this.data.markChangedAndNotify(this.chest);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items().get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        this.items().set(slot, ItemStack.EMPTY);
        this.data.markChangedAndNotify(this.chest);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items().set(slot, stack);
        this.data.markChangedAndNotify(this.chest);
    }

    @Override
    public void setChanged() {
        this.data.markChangedAndNotify(this.chest);
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
        this.data.onChestOpen(this.chest);
        this.chest.startOpen(user);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.data.onChestClose(this.chest);
        this.chest.stopOpen(user);
    }

    @Override
    public void clearContent() {
        // Never clear the shared global list from the menu wrapper (would empty all chests).
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return this.items().iterator();
    }
}

