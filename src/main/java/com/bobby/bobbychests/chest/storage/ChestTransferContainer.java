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

/**
 * The two item slots on a fluid or energy chest: one to put a bucket or battery into, one where the
 * emptied or filled result lands.
 *
 * <p>Deliberately not storage. It is a work surface for hand transfers, so that a player can fill a
 * tank without a pipe, and it is what {@link com.bobby.bobbychests.chest.menu.AbstractChestMenu#hasStoredItems}
 * ignores when deciding whether a chest is empty enough to change modes.
 */
public final class ChestTransferContainer implements Container {

    public static final int SIZE = 2;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final AbstractTieredChestBlockEntity chest;

    public ChestTransferContainer(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    public AbstractTieredChestBlockEntity getChest() {
        return this.chest;
    }

    private NonNullList<ItemStack> stacks() {
        return this.chest.getTransferItems();
    }

    @Override
    public int getContainerSize() {
        return SIZE;
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
            this.setChanged();
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
        this.setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stacks().set(slot, stack);
        this.setChanged();
    }

    /**
     * Both slots accept items at the container level.
     *
     * <p>It is tempting to reject the output slot here so nothing can be put in it by hand, but
     * {@code VanillaContainerWrapper} routes every insert through this method — including the
     * transfer writing an emptied bucket into the output. Rejecting it made the write fail, which
     * failed the extract, which meant buckets never filled or drained at all.
     *
     * <p>Manual placement is blocked where it belongs instead: the output {@code Slot} in
     * {@link com.bobby.bobbychests.chest.menu.resource.AbstractResourceChestMenu} returns false
     * from {@code mayPlace}.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void setChanged() {
        this.chest.onTransferSlotsChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.chest.canPlayerOpen(player) && Container.stillValidBlockEntity(this.chest, player);
    }

    /**
     * Opening a tank still opens the chest.
     *
     * <p>Delegates to the block entity exactly as {@link RoutedChestContainer} does, so the lid
     * animates and the chest sound plays in fluid and energy mode too. Left as no-ops originally,
     * which made a tank the only chest in the mod that opened silently and without moving.
     */
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
        NonNullList<ItemStack> stacks = this.stacks();
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return this.stacks().iterator();
    }
}
