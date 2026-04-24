package com.bobby.bobbychests.globalcheststorage;

import com.bobby.bobbychests.blockentity.FirstChestBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public class GlobalFirstChestContainer implements Container {

    private final GlobalFirstChestData data;
    private final FirstChestBlockEntity chest;
    public GlobalFirstChestContainer(GlobalFirstChestData data, FirstChestBlockEntity chest) {
        this.data = data;
        this.chest = chest;
    }

    private NonNullList<ItemStack> items() {
        return this.data.getItemsForChest(this.chest);
    }

    public FirstChestBlockEntity getChest() {
        return this.chest;
    }
    @Override
    public int getContainerSize() {
        return GlobalFirstChestData.SLOT_COUNT;
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
        // Decrement the shared viewer count before the BE triggers its close blockEvent,
        // so the lid can stay open if another chest on the same public key is still viewed.
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
