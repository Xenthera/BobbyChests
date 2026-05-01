package com.bobby.bobbychests.chest.menu;

import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class DeepStorageMenuActions {
    private DeepStorageMenuActions() {
    }

    static ItemStack quickMoveOutOfDeepStorage(AbstractChestMenu menu, int slotIndex, ItemStack marker, long deep) {
        int extracted = (int) Math.min((long) marker.getMaxStackSize(), deep);
        ItemStack clean = DeepStorageStacks.copyWithoutDeepCount(marker, extracted);
        if (!menu.moveItemStackToRange(clean, menu.getFirstPlayerSlotIndex(), menu.slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        int moved = extracted - clean.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        setDeepSlotCount(menu.getContainer(), slotIndex, marker, deep - (long) moved);
        return DeepStorageStacks.copyWithoutDeepCount(marker, moved);
    }

    static boolean tryQuickMoveOutOfDeepStorage(AbstractChestMenu menu, int slotIndex, ItemStack marker) {
        long deep = DeepStorageStacks.getDeepCount(marker);
        if (deep <= 0L) {
            return false;
        }
        quickMoveOutOfDeepStorage(menu, slotIndex, marker, deep);
        return true;
    }

    static boolean handleDeepStorageClick(AbstractChestMenu menu, int slotIndex, int button, ContainerInput containerInput, Player player) {
        Slot slot = menu.slots.get(slotIndex);
        ItemStack marker = slot.getItem();
        ItemStack carried = menu.getCarried();
        long deep = DeepStorageStacks.getDeepCount(marker);

        if (containerInput == ContainerInput.PICKUP) {
            if (deep <= 0L && !marker.isEmpty() && marker.getMaxStackSize() <= 1) {
                return false;
            }
            if (marker.isEmpty() && !carried.isEmpty() && carried.getMaxStackSize() <= 1) {
                return false;
            }
            if (carried.isEmpty()) {
                return pickupFromDeepSlot(menu, slotIndex, marker, button);
            }
            return placeIntoDeepSlot(menu, slotIndex, marker, carried, button);
        }

        if (containerInput == ContainerInput.THROW && carried.isEmpty()) {
            return dropFromDeepSlot(menu, player, slotIndex, marker, button);
        }

        if (containerInput == ContainerInput.QUICK_MOVE) {
            return tryQuickMoveOutOfDeepStorage(menu, slotIndex, marker);
        }

        return deep > 0L;
    }

    static boolean tryMoveIntoDeepStorage(AbstractChestMenu menu, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getMaxStackSize() <= 1) {
            return false;
        }
        if (!menu.canUseDeepStorageInMenu()) {
            return false;
        }

        Container container = menu.getContainer();
        for (int i = 0; i < menu.getChestSlotCount() && !stack.isEmpty(); i++) {
            ItemStack current = container.getItem(i);
            if (current.isEmpty()) {
                continue;
            }
            if (!DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(current, stack)) {
                continue;
            }
            long deep = DeepStorageStacks.getDeepCount(current);
            if (deep <= 0L) {
                deep = current.getCount();
            }
            container.setItem(i, DeepStorageStacks.makeDeepMarker(current, DeepStorageStacks.saturatedAdd(deep, stack.getCount())));
            stack.setCount(0);
            return true;
        }

        for (int i = 0; i < menu.getChestSlotCount() && !stack.isEmpty(); i++) {
            ItemStack current = container.getItem(i);
            if (!current.isEmpty()) {
                continue;
            }
            container.setItem(i, DeepStorageStacks.makeDeepMarker(stack, (long) stack.getCount()));
            stack.setCount(0);
            return true;
        }
        return false;
    }

    private static boolean pickupFromDeepSlot(AbstractChestMenu menu, int slotIndex, ItemStack marker, int button) {
        long deep = DeepStorageStacks.getDeepCount(marker);
        if (deep <= 0L) {
            return false;
        }
        int extracted = button == 1 ? 1 : (int) Math.min((long) marker.getMaxStackSize(), deep);
        menu.setCarried(DeepStorageStacks.copyWithoutDeepCount(marker, extracted));
        setDeepSlotCount(menu.getContainer(), slotIndex, marker, deep - extracted);
        return true;
    }

    private static boolean placeIntoDeepSlot(AbstractChestMenu menu, int slotIndex, ItemStack marker, ItemStack carried, int button) {
        if (carried.getMaxStackSize() <= 1) {
            return true;
        }
        long deep = DeepStorageStacks.getDeepCount(marker);
        if (!marker.isEmpty() && deep <= 0L) {
            deep = marker.getCount();
        }
        if (!marker.isEmpty() && !DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(marker, carried)) {
            return true;
        }

        int wanted = button == 1 ? 1 : carried.getCount();
        int inserted = (int) Math.min((long) wanted, Long.MAX_VALUE - deep);
        if (inserted <= 0) {
            return true;
        }
        ItemStack base = marker.isEmpty() ? carried : marker;
        menu.getContainer().setItem(slotIndex, DeepStorageStacks.makeDeepMarker(base, deep + (long) inserted));
        carried.shrink(inserted);
        if (carried.isEmpty()) {
            menu.setCarried(ItemStack.EMPTY);
        }
        return true;
    }

    private static boolean dropFromDeepSlot(AbstractChestMenu menu, Player player, int slotIndex, ItemStack marker, int button) {
        long deep = DeepStorageStacks.getDeepCount(marker);
        if (deep <= 0L) {
            return false;
        }
        int extracted = button == 1 ? (int) Math.min((long) marker.getMaxStackSize(), deep) : 1;
        ItemStack clean = DeepStorageStacks.copyWithoutDeepCount(marker, extracted);
        player.drop(clean, true);
        setDeepSlotCount(menu.getContainer(), slotIndex, marker, deep - extracted);
        return true;
    }

    private static void setDeepSlotCount(Container container, int slotIndex, ItemStack marker, long count) {
        if (count <= 0L) {
            container.setItem(slotIndex, ItemStack.EMPTY);
        } else {
            container.setItem(slotIndex, DeepStorageStacks.makeDeepMarker(marker, count));
        }
    }
}
