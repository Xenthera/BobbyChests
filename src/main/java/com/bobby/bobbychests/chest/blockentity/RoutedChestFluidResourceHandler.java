package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid transfer capability backed by the chest's active storage route.
 *
 * <p>The fluid counterpart to {@link RoutedChestItemResourceHandler}: one tank rather than a slot
 * list, but the same routing (local holder or shared global pool, decided per call so a channel
 * change takes effect immediately) and the same upgrade-card behaviour — the infinite card makes
 * the tank a bottomless source and sink, and the void card swallows anything that will not fit.
 */
final class RoutedChestFluidResourceHandler implements ResourceHandler<FluidResource> {

    private final AbstractTieredChestBlockEntity chest;
    private final TankJournal journal;

    RoutedChestFluidResourceHandler(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
        this.journal = new TankJournal(chest);
    }

    private @Nullable ChestResourceContents contents() {
        if (!(this.chest.getLevel() instanceof ServerLevel)) {
            return null;
        }
        if (this.chest.getResourceMode() != ChestResourceMode.FLUID) {
            return null;
        }
        return this.chest.getActiveResources();
    }

    private int capacity() {
        return this.chest.getFluidCapacityMb();
    }

    private boolean canExtractInfinitely() {
        return this.chest.getUpgradeManager().capabilities().canExtractInfinitely();
    }

    private boolean canVoidWhenFull() {
        return this.chest.getUpgradeManager().capabilities().canVoidWhenFull();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int slot) {
        ChestResourceContents contents = this.contents();
        return contents == null ? FluidResource.EMPTY : contents.fluid();
    }

    @Override
    public long getAmountAsLong(int slot) {
        ChestResourceContents contents = this.contents();
        if (contents == null || contents.isFluidEmpty()) {
            return 0L;
        }
        // An infinite tank reads as full so pipes size their requests against the whole buffer.
        return this.canExtractInfinitely() ? this.capacity() : contents.fluidAmount();
    }

    @Override
    public long getCapacityAsLong(int slot, FluidResource resource) {
        return this.contents() == null ? 0L : this.capacity();
    }

    @Override
    public boolean isValid(int slot, FluidResource resource) {
        return this.contents() != null;
    }

    @Override
    public int insert(int slot, FluidResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ChestResourceContents contents = this.contents();
        if (contents == null) {
            return 0;
        }
        boolean empty = contents.isFluidEmpty();
        if (!empty && !contents.fluid().equals(resource)) {
            // Wrong fluid. A void tank still swallows it rather than backing the pipe up.
            return this.canVoidWhenFull() ? amount : 0;
        }
        if (this.canExtractInfinitely()) {
            // Bottomless sink: report everything accepted without growing the stored amount.
            return amount;
        }

        int current = empty ? 0 : contents.fluidAmount();
        int accepted = Math.min(amount, this.capacity() - current);
        if (accepted <= 0) {
            return this.canVoidWhenFull() ? amount : 0;
        }

        this.journal.updateSnapshots(tx);
        contents.setFluid(resource, current + accepted);
        // A void tank reports the overflow as accepted too, and drops it on the floor.
        return this.canVoidWhenFull() ? amount : accepted;
    }

    @Override
    public int extract(int slot, FluidResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ChestResourceContents contents = this.contents();
        if (contents == null || contents.isFluidEmpty()) {
            return 0;
        }
        if (!contents.fluid().equals(resource)) {
            return 0;
        }
        if (this.canExtractInfinitely()) {
            // Bottomless source: hand over whatever was asked for and leave the tank untouched.
            return Math.min(amount, this.capacity());
        }

        int extracted = Math.min(amount, contents.fluidAmount());
        if (extracted <= 0) {
            return 0;
        }
        this.journal.updateSnapshots(tx);
        contents.setFluid(contents.fluid(), contents.fluidAmount() - extracted);
        return extracted;
    }

    /**
     * Snapshots the whole holder rather than a single field, because an insert into an empty tank
     * changes the fluid identity and the amount together and a rollback has to undo both.
     */
    private static final class TankJournal extends SnapshotJournal<ChestResourceContents> {
        private final AbstractTieredChestBlockEntity chest;

        private TankJournal(AbstractTieredChestBlockEntity chest) {
            this.chest = chest;
        }

        @Override
        protected ChestResourceContents createSnapshot() {
            return this.chest.getActiveResources().copy();
        }

        @Override
        protected void revertToSnapshot(ChestResourceContents snapshot) {
            this.chest.getActiveResources().copyFrom(snapshot);
        }

        @Override
        protected void onRootCommit(ChestResourceContents snapshot) {
            this.chest.onResourcesChanged();
        }
    }
}
