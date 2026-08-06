package com.bobby.bobbychests.chest.storage;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.upgrade.ChestModeCards;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

/**
 * The single mode-card slot: one card, deciding whether the chest stores items, fluid, or FE.
 *
 * <p>Separate from the upgrade slots so changing a chest's mode costs nothing. A dirt chest has one
 * upgrade slot, and spending it on being a tank rather than on an actual upgrade was a poor trade to
 * force on anyone.
 */
public final class ChestModeContainer implements Container {

    public static final int SIZE = 1;

    private final AbstractTieredChestBlockEntity chest;

    public ChestModeContainer(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    public AbstractTieredChestBlockEntity getChest() {
        return this.chest;
    }

    private NonNullList<ItemStack> stacks() {
        return this.chest.getModeItems();
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.stacks().get(0).isEmpty();
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

    /** Only mode cards, so automation cannot stuff an upgrade in here either. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return ChestModeCards.isModeCard(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        this.chest.onModeCardChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.chest.canPlayerOpen(player) && Container.stillValidBlockEntity(this.chest, player);
    }

    @Override
    public void startOpen(ContainerUser user) {
    }

    @Override
    public void stopOpen(ContainerUser user) {
    }

    @Override
    public void clearContent() {
        this.stacks().set(0, ItemStack.EMPTY);
        this.setChanged();
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return this.stacks().iterator();
    }
}
