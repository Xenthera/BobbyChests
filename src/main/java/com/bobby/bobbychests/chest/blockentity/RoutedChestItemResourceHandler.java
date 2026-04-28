package com.bobby.bobbychests.chest.blockentity;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Item transfer capability backed by the chest's active storage route.
 */
final class RoutedChestItemResourceHandler implements ResourceHandler<ItemResource> {
    private final AbstractTieredChestBlockEntity chest;
    private final ArrayList<SlotJournal> journals = new ArrayList<>();

    RoutedChestItemResourceHandler(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    private @Nullable NonNullList<ItemStack> items() {
        if (!(this.chest.getLevel() instanceof ServerLevel)) {
            return null;
        }
        return this.chest.getActiveItems();
    }

    private SlotJournal journal(int slot) {
        this.journals.ensureCapacity(slot + 1);
        while (this.journals.size() <= slot) {
            this.journals.add(new SlotJournal(this.chest, this.journals.size()));
        }
        return this.journals.get(slot);
    }

    private static int capacity(ItemResource resource) {
        return resource.isEmpty() ? 99 : Math.min(resource.getMaxStackSize(), 99);
    }

    @Override
    public int size() {
        NonNullList<ItemStack> items = this.items();
        return items == null ? 0 : items.size();
    }

    @Override
    public ItemResource getResource(int slot) {
        NonNullList<ItemStack> items = this.items();
        return items == null ? ItemResource.EMPTY : ItemResource.of(items.get(slot));
    }

    @Override
    public long getAmountAsLong(int slot) {
        NonNullList<ItemStack> items = this.items();
        return items == null ? 0L : items.get(slot).getCount();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        NonNullList<ItemStack> items = this.items();
        if (items == null) {
            return 0L;
        }
        if (!resource.isEmpty() && !this.isValid(slot, resource)) {
            return 0L;
        }
        return capacity(resource);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return this.items() != null;
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        NonNullList<ItemStack> items = this.items();
        if (items == null) {
            return 0;
        }
        ItemStack current = items.get(slot);
        int currentAmount = current.getCount();
        if (currentAmount > 0 && !resource.matches(current)) {
            return 0;
        }

        int inserted = Math.min(amount, capacity(resource) - currentAmount);
        if (inserted <= 0) {
            return 0;
        }

        this.journal(slot).updateSnapshots(tx);
        items.set(slot, resource.toStack(currentAmount + inserted));
        return inserted;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        NonNullList<ItemStack> items = this.items();
        if (items == null) {
            return 0;
        }
        ItemStack current = items.get(slot);
        if (!resource.matches(current)) {
            return 0;
        }

        int extracted = Math.min(amount, current.getCount());
        if (extracted <= 0) {
            return 0;
        }

        this.journal(slot).updateSnapshots(tx);
        int remaining = current.getCount() - extracted;
        items.set(slot, remaining <= 0 ? ItemStack.EMPTY : resource.toStack(remaining));
        return extracted;
    }

    private static final class SlotJournal extends SnapshotJournal<ItemStack> {
        private final AbstractTieredChestBlockEntity chest;
        private final int slot;

        private SlotJournal(AbstractTieredChestBlockEntity chest, int slot) {
            this.chest = chest;
            this.slot = slot;
        }

        private NonNullList<ItemStack> items() {
            return this.chest.getActiveItems();
        }

        @Override
        protected ItemStack createSnapshot() {
            return this.items().get(this.slot).copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            this.items().set(this.slot, snapshot.copy());
        }

        @Override
        protected void onRootCommit(ItemStack snapshot) {
            this.chest.setChanged();
        }
    }
}
