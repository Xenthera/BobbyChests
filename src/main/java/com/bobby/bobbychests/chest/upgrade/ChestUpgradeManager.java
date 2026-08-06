package com.bobby.bobbychests.chest.upgrade;

import com.bobby.bobbychests.chest.ChestTier;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.datagen.BobbyChestTags;
import com.bobby.bobbychests.item.ChestUpgradeCardItem;
import com.bobby.bobbychests.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ChestUpgradeManager {
    /** @deprecated Use {@link ChestTier#MAX_UPGRADE_SLOTS} / {@link ChestTier#upgradeSlotCount()}. */
    @Deprecated
    public static final int SLOT_COUNT = ChestTier.MAX_UPGRADE_SLOTS;

    private static final String TAG_UPGRADES = "upgrades";

    private final AbstractTieredChestBlockEntity chest;
    private final NonNullList<ItemStack> stacks;

    public ChestUpgradeManager(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
        this.stacks = NonNullList.withSize(chest.getTier().upgradeSlotCount(), ItemStack.EMPTY);
    }

    public AbstractTieredChestBlockEntity getChest() {
        return this.chest;
    }

    public int getSlotCount() {
        return this.stacks.size();
    }

    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    public ItemStack getItem(int slot) {
        return this.stacks.get(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
        this.onContentsChanged();
    }

    public static boolean isUpgradeCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof ChestUpgradeCardItem || stack.is(BobbyChestTags.UPGRADE_CARDS);
    }

    /** True when any upgrade slot holds {@code card}. */
    private boolean has(Item card) {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty() && stack.getItem() == card) {
                return true;
            }
        }
        return false;
    }

    /** Current capabilities from whichever upgrade slots hold matching cards (any slot). */
    public ChestUpgradeCapabilities capabilities() {
        return new ChestUpgradeCapabilities(
                this.has(ModItems.NETWORKING_UPGRADE_CARD.get()),
                this.has(ModItems.INFINITE_UPGRADE_CARD.get()),
                this.has(ModItems.VOID_UPGRADE_CARD.get()),
                this.has(ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get()),
                this.has(ModItems.RETAIN_ITEMS_UPGRADE_CARD.get()),
                this.has(ModItems.LOCK_UPGRADE_CARD.get()),
                this.has(ModItems.DEEP_STORAGE_UPGRADE_CARD.get()),
                this.has(ModItems.FLUID_UPGRADE_CARD.get()),
                this.has(ModItems.ENERGY_UPGRADE_CARD.get()));
    }

    public void onContentsChanged() {
        this.chest.setChanged();
        this.chest.onUpgradeInventoryChanged();
    }

    public void load(ValueInput input) {
        ValueInput child = input.childOrEmpty(TAG_UPGRADES);
        NonNullList<ItemStack> loaded = NonNullList.withSize(ChestTier.MAX_UPGRADE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(child, loaded);

        for (int i = 0; i < this.stacks.size(); i++) {
            this.stacks.set(i, ItemStack.EMPTY);
        }

        Level level = this.chest.getLevel();
        BlockPos pos = this.chest.getBlockPos();
        for (int i = 0; i < loaded.size(); i++) {
            ItemStack stack = loaded.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (i < this.stacks.size()) {
                this.stacks.set(i, stack);
            } else if (level != null && !level.isClientSide()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    public void save(ValueOutput output) {
        ValueOutput child = output.child(TAG_UPGRADES);
        ContainerHelper.saveAllItems(child, this.stacks);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        for (int i = 0; i < this.stacks.size(); i++) {
            this.stacks.set(i, ItemStack.EMPTY);
        }
        this.chest.setChanged();
        this.chest.onUpgradeInventoryChanged();
    }

    public boolean stillValid(Player player) {
        return this.chest.canPlayerOpen(player)
                && net.minecraft.world.Container.stillValidBlockEntity(this.chest, player);
    }
}
