package com.bobby.bobbychests.chest.storage;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import net.minecraft.world.Container;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.HandlerItemAccess;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Moves fluid or FE between the item in a chest's input transfer slot and the chest itself.
 *
 * <p>Runs both directions: a full bucket empties into the tank, an empty one fills from it, and the
 * same for charged and empty batteries. Direction is decided by trying to drain the item first and
 * falling back to filling it, which is what makes a single pair of slots handle both without a mode
 * switch the player has to think about.
 */
public final class ChestResourceTransfer {

    /**
     * How many times one handler is asked to move before we accept it is done.
     *
     * <p>Same reasoning as BobbyPipes' {@code FluidAccess}: handlers routinely cap a single call at
     * a per-operation rate, so believing one call under-transfers. The bound only stops a
     * pathological handler from spinning forever.
     */
    private static final int PUMP_ATTEMPTS = 64;

    /** Upper bound on items converted per change event; a stack of buckets is at most 16. */
    private static final int MAX_PASSES = 64;

    private ChestResourceTransfer() {
    }

    /**
     * Runs one transfer pass for {@code chest}.
     *
     * @return true if anything moved, in which case the caller should mark the chest changed
     */
    /**
     * Runs the same transfer against an arbitrary item location rather than the transfer slots.
     *
     * <p>Used by shift-clicking a bucket in the player's inventory: the access points at that
     * inventory slot, so a drained bucket is handed straight back to the player instead of routing
     * through the chest's own slots.
     *
     * @return true if anything moved
     */
    public static boolean runAgainst(AbstractTieredChestBlockEntity chest, ItemAccess access) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE) {
            return false;
        }
        if (access.getResource().isEmpty()) {
            return false;
        }
        // Exactly one pass, and callers pass a one-at-a-time access, so a single interaction moves a
        // single bucket. The transfer slots loop instead — dropping a stack in there is a deliberate
        // "process all of this", whereas a click on the block is one click, one bucket.
        return switch (chest.getResourceMode()) {
            case FLUID -> runFluid(chest, access);
            case ENERGY -> runEnergy(chest, access);
            case ITEM -> false;
        };
    }

    public static boolean run(AbstractTieredChestBlockEntity chest, Container transferSlots) {
        // A transfer needs its own root transaction, and opening one while another is live throws.
        // This is reachable: a pipe inserting into the tank commits, the commit hook reports the
        // change, and the chest asks whether a bucket in the input slot can now be filled — all
        // while the pipe's own transaction is still closing. Skipping is the right answer rather
        // than deferring, because whatever the pipe does next will report a change again.
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE) {
            return false;
        }

        ResourceHandler<ItemResource> slots = VanillaContainerWrapper.of(transferSlots);
        // One item at a time. An item-backed handler scales its whole capacity by the stack count,
        // so a stack of five buckets would be an all-or-nothing 5000 mB transfer that silently does
        // nothing whenever the tank has room for less than all of it.
        ItemAccess access = new InputToOutputItemAccess(slots).oneByOne();

        // Then repeat, so dropping in a whole stack empties as much of it as fits rather than
        // converting one bucket per click. Stops as soon as a pass moves nothing, which covers a
        // full tank, an empty one, and a full output slot alike.
        boolean movedAny = false;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            if (access.getResource().isEmpty()) {
                break;
            }
            boolean moved = switch (chest.getResourceMode()) {
                case FLUID -> runFluid(chest, access);
                case ENERGY -> runEnergy(chest, access);
                case ITEM -> false;
            };
            if (!moved) {
                break;
            }
            movedAny = true;
        }
        return movedAny;
    }

    private static boolean runFluid(AbstractTieredChestBlockEntity chest, ItemAccess access) {
        ResourceHandler<FluidResource> tank = chest.getFluidResourceHandler(null);
        if (tank == null) {
            return false;
        }
        ResourceHandler<FluidResource> itemTank = access.getCapability(Capabilities.Fluid.ITEM);
        if (itemTank == null) {
            return false;
        }

        // Item into tank first: a player holding a full bucket over a partly filled tank means to
        // empty it, not to top the bucket up.
        try (Transaction tx = Transaction.openRoot()) {
            if (moveFluid(itemTank, tank, tx) > 0) {
                tx.commit();
                return true;
            }
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (moveFluid(tank, itemTank, tx) > 0) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    /** Drains every non-empty slot of {@code from} into {@code into}, returning the mB moved. */
    private static int moveFluid(
            ResourceHandler<FluidResource> from,
            ResourceHandler<FluidResource> into,
            TransactionContext tx) {
        int moved = 0;
        for (int slot = 0; slot < from.size(); slot++) {
            FluidResource resource = from.getResource(slot);
            if (resource.isEmpty()) {
                continue;
            }
            int available = from.getAmountAsInt(slot);
            if (available <= 0) {
                continue;
            }
            // Probe how much the destination will really take before pulling anything out, so a
            // full tank cannot swallow half a bucket and lose the rest.
            int accepted;
            try (Transaction probe = Transaction.open(tx)) {
                accepted = pumpInsert(into, resource, available, probe);
                // Rolled back: this was a capacity check.
            }
            if (accepted <= 0) {
                continue;
            }
            int taken = pumpExtract(from, slot, resource, accepted, tx);
            if (taken <= 0) {
                continue;
            }
            int placed = pumpInsert(into, resource, taken, tx);
            if (placed < taken) {
                // Destination changed its mind between probe and commit; abandon the whole pass
                // rather than destroying the difference.
                return 0;
            }
            moved += placed;
        }
        return moved;
    }

    private static int pumpInsert(
            ResourceHandler<FluidResource> handler, FluidResource resource, int wanted, TransactionContext tx) {
        int placed = 0;
        for (int attempt = 0; attempt < PUMP_ATTEMPTS && placed < wanted; attempt++) {
            int step = handler.insert(resource, wanted - placed, tx);
            if (step <= 0) {
                break;
            }
            placed += step;
        }
        return placed;
    }

    private static int pumpExtract(
            ResourceHandler<FluidResource> handler, int slot, FluidResource resource, int wanted, TransactionContext tx) {
        int taken = 0;
        for (int attempt = 0; attempt < PUMP_ATTEMPTS && taken < wanted; attempt++) {
            int step = handler.extract(slot, resource, wanted - taken, tx);
            if (step <= 0) {
                break;
            }
            taken += step;
        }
        return taken;
    }

    private static boolean runEnergy(AbstractTieredChestBlockEntity chest, ItemAccess access) {
        EnergyHandler buffer = chest.getEnergyHandler(null);
        if (buffer == null) {
            return false;
        }
        EnergyHandler itemBuffer = access.getCapability(Capabilities.Energy.ITEM);
        if (itemBuffer == null) {
            return false;
        }

        // Charge the chest from the item first, mirroring the fluid direction preference.
        try (Transaction tx = Transaction.openRoot()) {
            if (moveEnergy(itemBuffer, buffer, tx) > 0) {
                tx.commit();
                return true;
            }
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (moveEnergy(buffer, itemBuffer, tx) > 0) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    private static int moveEnergy(EnergyHandler from, EnergyHandler into, TransactionContext tx) {
        int available = from.getAmountAsInt();
        if (available <= 0) {
            return 0;
        }
        int accepted;
        try (Transaction probe = Transaction.open(tx)) {
            accepted = into.insert(available, probe);
            // Rolled back: capacity check only.
        }
        if (accepted <= 0) {
            return 0;
        }
        int taken = from.extract(accepted, tx);
        if (taken <= 0) {
            return 0;
        }
        int placed = into.insert(taken, tx);
        return placed < taken ? 0 : placed;
    }

    /**
     * Reads the input slot but writes results to the output slot.
     *
     * <p>The default {@link HandlerItemAccess} puts a transformed item back where it came from,
     * which for these slots would drop an emptied bucket straight back into the input and refill it
     * on the next pass. Sending results to the output slot instead is what makes the pair terminate.
     */
    private static final class InputToOutputItemAccess extends HandlerItemAccess {
        private InputToOutputItemAccess(ResourceHandler<ItemResource> slots) {
            super(slots, ChestTransferContainer.INPUT_SLOT);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return this.handler.insert(ChestTransferContainer.OUTPUT_SLOT, resource, amount, transaction);
        }
    }
}
