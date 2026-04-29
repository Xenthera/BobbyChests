package com.bobby.bobbychests.chest.upgrade;

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
    public static final int SLOT_COUNT = 3;
    private static final String TAG_UPGRADES = "upgrades";

    private final AbstractTieredChestBlockEntity chest;
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public ChestUpgradeManager(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    public AbstractTieredChestBlockEntity getChest() {
        return this.chest;
    }

    public int getSlotCount() {
        return SLOT_COUNT;
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

    /** Current capabilities from whichever upgrade slots hold matching cards (any slot). */
    public ChestUpgradeCapabilities capabilities() {
        Item networking = ModItems.NETWORKING_UPGRADE_CARD.get();
        boolean networkingPresent = false;
        for (ItemStack stack : this.stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == networking) {
                networkingPresent = true;
                break;
            }
        }
        return new ChestUpgradeCapabilities(networkingPresent);
    }

    public void onContentsChanged() {
        this.chest.setChanged();
        this.chest.onUpgradeInventoryChanged();
    }

    public void load(ValueInput input) {
        ValueInput child = input.childOrEmpty(TAG_UPGRADES);
        ContainerHelper.loadAllItems(child, this.stacks);
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
