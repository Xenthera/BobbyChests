package com.bobby.bobbychests.chest.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The non-item contents of one storage: a single fluid stack and a single FE amount.
 *
 * <p>Mutable on purpose. A chest in {@link ChestStorageMode#LOCAL} owns one of these directly,
 * and a chest in {@link ChestStorageMode#GLOBAL} borrows the one
 * {@link GlobalTieredChestData} holds for its storage key. Both paths then mutate the same
 * shape, which is what lets the capability handlers stay unaware of which mode they are on.
 *
 * <p>Fluid and energy share a holder even though a chest is only ever one of the two. Keeping
 * them together means one map, one codec entry, and one lookup in the global data instead of
 * two parallel copies of all of it. They are still wholly separate from the item storage, so a
 * fluid chest on channel 5 can never surface what an item chest on channel 5 is holding.
 */
public final class ChestResourceContents {

    public static final Codec<ChestResourceContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidResource.OPTIONAL_CODEC.optionalFieldOf("Fluid", FluidResource.EMPTY).forGetter(ChestResourceContents::fluid),
            Codec.INT.optionalFieldOf("FluidAmount", 0).forGetter(ChestResourceContents::fluidAmount),
            Codec.INT.optionalFieldOf("Energy", 0).forGetter(ChestResourceContents::energy)
    ).apply(instance, ChestResourceContents::new));

    private FluidResource fluid;
    private int fluidAmount;
    private int energy;

    public ChestResourceContents() {
        this(FluidResource.EMPTY, 0, 0);
    }

    public ChestResourceContents(FluidResource fluid, int fluidAmount, int energy) {
        this.fluid = fluid == null ? FluidResource.EMPTY : fluid;
        this.fluidAmount = Math.max(0, fluidAmount);
        this.energy = Math.max(0, energy);
        this.normalize();
    }

    /** An empty fluid can never carry an amount, and an amount of zero can never carry an identity. */
    private void normalize() {
        if (this.fluid.isEmpty() || this.fluidAmount <= 0) {
            this.fluid = FluidResource.EMPTY;
            this.fluidAmount = 0;
        }
    }

    public FluidResource fluid() {
        return this.fluid;
    }

    public int fluidAmount() {
        return this.fluidAmount;
    }

    public int energy() {
        return this.energy;
    }

    public boolean isFluidEmpty() {
        return this.fluid.isEmpty() || this.fluidAmount <= 0;
    }

    public void setFluid(FluidResource fluid, int amount) {
        this.fluid = fluid == null ? FluidResource.EMPTY : fluid;
        this.fluidAmount = Math.max(0, amount);
        this.normalize();
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy);
    }

    public void clear() {
        this.fluid = FluidResource.EMPTY;
        this.fluidAmount = 0;
        this.energy = 0;
    }

    public ChestResourceContents copy() {
        return new ChestResourceContents(this.fluid, this.fluidAmount, this.energy);
    }

    public void copyFrom(ChestResourceContents other) {
        this.setFluid(other.fluid, other.fluidAmount);
        this.setEnergy(other.energy);
    }

    public boolean isEmpty() {
        return this.isFluidEmpty() && this.energy <= 0;
    }
}
