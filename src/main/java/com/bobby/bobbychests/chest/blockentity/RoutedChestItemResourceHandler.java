package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
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

    private boolean canUseDeepStorage() {
        return this.chest.canUseDeepStorage();
    }

    private boolean canExtractInfinitely() {
        return this.chest.getUpgradeManager().capabilities().canExtractInfinitely();
    }

    private boolean canVoidWhenFull() {
        return this.chest.getUpgradeManager().capabilities().canVoidWhenFull();
    }

    private boolean canLeaveLastItemForAutomation() {
        return this.chest.getUpgradeManager().capabilities().canLeaveLastItemForAutomation();
    }

    private static boolean matchesIgnoringDeepCount(ItemResource resource, ItemStack stack) {
        return resource.matches(stack) || DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(resource.toStack(1), stack);
    }

    private boolean hasAnyInsertSpace(NonNullList<ItemStack> items, ItemResource resource) {
        int cap = capacity(resource);
        for (ItemStack current : items) {
            if (current.isEmpty()) {
                return true;
            }
            if (matchesIgnoringDeepCount(resource, current) && current.getCount() < cap) {
                return true;
            }
        }
        return false;
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
        if (items == null) {
            return 0L;
        }
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            return 0L;
        }
        if (this.canUseDeepStorage()) {
            long deep = DeepStorageStacks.getDeepCount(current);
            if (deep > 0L) {
                return deep;
            }
        }
        return this.canExtractInfinitely() ? current.getMaxStackSize() : current.getCount();
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
        if (this.canUseDeepStorage() && !resource.isEmpty() && resource.getMaxStackSize() > 1) {
            return Long.MAX_VALUE;
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
        if (this.canUseDeepStorage() && resource.getMaxStackSize() > 1) {
            ItemStack current = items.get(slot);
            long currentDeep = DeepStorageStacks.getDeepCount(current);
            if (!current.isEmpty() && currentDeep <= 0L && current.getMaxStackSize() > 1) {
                // Upgrade an existing normal stack into a deep-storage marker.
                currentDeep = current.getCount();
            }
            if (!current.isEmpty() && !matchesIgnoringDeepCount(resource, current)) {
                return this.canVoidWhenFull() && !this.hasAnyInsertSpace(items, resource) ? amount : 0;
            }

            long next = currentDeep + (long) amount;
            int inserted = amount;
            if (next < 0L || next > Long.MAX_VALUE) {
                // overflow safety; cap
                long remainingCap = Long.MAX_VALUE - currentDeep;
                inserted = (int) Math.min((long) amount, Math.max(0L, remainingCap));
                next = currentDeep + inserted;
            }
            if (inserted <= 0) {
                return this.canVoidWhenFull() && !this.hasAnyInsertSpace(items, resource) ? amount : 0;
            }

            this.journal(slot).updateSnapshots(tx);
            ItemStack base = current.isEmpty() ? resource.toStack(1) : current;
            items.set(slot, DeepStorageStacks.makeDeepMarker(base, next));
            return inserted;
        }
        ItemStack current = items.get(slot);
        int currentAmount = current.getCount();
        if (currentAmount > 0 && !matchesIgnoringDeepCount(resource, current)) {
            return this.canVoidWhenFull() && !this.hasAnyInsertSpace(items, resource) ? amount : 0;
        }

        int inserted = Math.min(amount, capacity(resource) - currentAmount);
        if (inserted <= 0) {
            return this.canVoidWhenFull() && !this.hasAnyInsertSpace(items, resource) ? amount : 0;
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
        if (!matchesIgnoringDeepCount(resource, current)) {
            return 0;
        }
        if (this.canUseDeepStorage()) {
            long deep = DeepStorageStacks.getDeepCount(current);
            if (deep > 0L) {
                long available = deep;
                if (this.canLeaveLastItemForAutomation() && available <= 1L) {
                    return 0;
                }
                if (this.canLeaveLastItemForAutomation()) {
                    available = Math.max(0L, available - 1L);
                }
                int extracted = (int) Math.min((long) amount, available);
                if (extracted <= 0) {
                    return 0;
                }
                this.journal(slot).updateSnapshots(tx);
                long next = deep - extracted;
                if (next <= 0L) {
                    items.set(slot, ItemStack.EMPTY);
                } else {
                    items.set(slot, DeepStorageStacks.makeDeepMarker(current, next));
                }
                return extracted;
            }
        }

        if (!this.canExtractInfinitely() && this.canLeaveLastItemForAutomation()) {
            if (current.getCount() <= 1) {
                return 0;
            }
        }

        int available = this.canExtractInfinitely()
                ? capacity(resource)
                : (this.canLeaveLastItemForAutomation() ? Math.max(0, current.getCount() - 1) : current.getCount());
        int extracted = Math.min(amount, available);
        if (extracted <= 0) {
            return 0;
        }

        if (this.canExtractInfinitely()) {
            return extracted;
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
