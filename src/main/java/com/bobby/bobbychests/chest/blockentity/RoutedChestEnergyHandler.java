package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * FE transfer capability backed by the chest's active storage route.
 *
 * <p>Same routing and card handling as {@link RoutedChestFluidResourceHandler}, minus resource
 * identity: energy is a bare amount, so there is nothing to match on and no wrong-resource case.
 */
final class RoutedChestEnergyHandler implements EnergyHandler {

    private final AbstractTieredChestBlockEntity chest;
    private final BufferJournal journal;

    RoutedChestEnergyHandler(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
        this.journal = new BufferJournal(chest);
    }

    private @Nullable ChestResourceContents contents() {
        if (!(this.chest.getLevel() instanceof ServerLevel)) {
            return null;
        }
        if (this.chest.getResourceMode() != ChestResourceMode.ENERGY) {
            return null;
        }
        return this.chest.getActiveResources();
    }

    private int capacity() {
        return this.chest.getEnergyCapacityFe();
    }

    private boolean canExtractInfinitely() {
        return this.chest.getUpgradeManager().capabilities().canExtractInfinitely();
    }

    private boolean canVoidWhenFull() {
        return this.chest.getUpgradeManager().capabilities().canVoidWhenFull();
    }

    @Override
    public long getAmountAsLong() {
        ChestResourceContents contents = this.contents();
        if (contents == null) {
            return 0L;
        }
        return this.canExtractInfinitely() ? this.capacity() : contents.energy();
    }

    @Override
    public long getCapacityAsLong() {
        return this.contents() == null ? 0L : this.capacity();
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        TransferPreconditions.checkNonNegative(amount);
        ChestResourceContents contents = this.contents();
        if (contents == null) {
            return 0;
        }
        if (this.canExtractInfinitely()) {
            return amount;
        }

        int accepted = Math.min(amount, this.capacity() - contents.energy());
        if (accepted <= 0) {
            return this.canVoidWhenFull() ? amount : 0;
        }

        this.journal.updateSnapshots(tx);
        contents.setEnergy(contents.energy() + accepted);
        return this.canVoidWhenFull() ? amount : accepted;
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        TransferPreconditions.checkNonNegative(amount);
        ChestResourceContents contents = this.contents();
        if (contents == null) {
            return 0;
        }
        if (this.canExtractInfinitely()) {
            return Math.min(amount, this.capacity());
        }

        int extracted = Math.min(amount, contents.energy());
        if (extracted <= 0) {
            return 0;
        }
        this.journal.updateSnapshots(tx);
        contents.setEnergy(contents.energy() - extracted);
        return extracted;
    }

    private static final class BufferJournal extends SnapshotJournal<ChestResourceContents> {
        private final AbstractTieredChestBlockEntity chest;

        private BufferJournal(AbstractTieredChestBlockEntity chest) {
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
