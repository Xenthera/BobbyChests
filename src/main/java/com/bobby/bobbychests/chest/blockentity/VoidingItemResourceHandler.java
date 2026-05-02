package com.bobby.bobbychests.chest.blockentity;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Adds one extra insert-only slot while the void upgrade is installed.
 * Pipes can target that slot when the real inventory is full; inserted items are discarded.
 */
final class VoidingItemResourceHandler implements ResourceHandler<ItemResource> {
    private static final int SOFT_SLOT_CAP = 99;

    private final AbstractTieredChestBlockEntity chest;
    private final ResourceHandler<ItemResource> delegate;

    VoidingItemResourceHandler(AbstractTieredChestBlockEntity chest, ResourceHandler<ItemResource> delegate) {
        this.chest = chest;
        this.delegate = delegate;
    }

    private int voidSlotIndex() {
        return this.delegate.size();
    }

    private static int capacity(ItemResource resource) {
        return resource.isEmpty() ? SOFT_SLOT_CAP : Math.min(resource.getMaxStackSize(), SOFT_SLOT_CAP);
    }

    @Override
    public int size() {
        return this.delegate.size() + 1;
    }

    @Override
    public ItemResource getResource(int slot) {
        return slot == this.voidSlotIndex() ? ItemResource.EMPTY : this.delegate.getResource(slot);
    }

    @Override
    public long getAmountAsLong(int slot) {
        return slot == this.voidSlotIndex() ? 0L : this.delegate.getAmountAsLong(slot);
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        return slot == this.voidSlotIndex() ? capacity(resource) : this.delegate.getCapacityAsLong(slot, resource);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        if (slot == this.voidSlotIndex()) {
            return this.chest.getLevel() instanceof ServerLevel
                    && this.chest.getUpgradeManager().capabilities().canVoidWhenFull();
        }
        return this.delegate.isValid(slot, resource);
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
        if (slot == this.voidSlotIndex()) {
            return this.chest.getUpgradeManager().capabilities().canVoidWhenFull() ? amount : 0;
        }
        return this.delegate.insert(slot, resource, amount, tx);
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        return slot == this.voidSlotIndex() ? 0 : this.delegate.extract(slot, resource, amount, tx);
    }
}
