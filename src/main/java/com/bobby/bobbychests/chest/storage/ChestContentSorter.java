package com.bobby.bobbychests.chest.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;

public final class ChestContentSorter {
    private ChestContentSorter() {
    }

    public static void sort(NonNullList<ItemStack> activeItems, int size, boolean deepStorageActive) {
        if (deepStorageActive) {
            sortDeepStorage(activeItems, size);
        } else {
            sortNormal(activeItems, size);
        }
    }

    private static void sortNormal(NonNullList<ItemStack> activeItems, int size) {
        ArrayList<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = activeItems.get(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        ArrayList<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isStackable()) {
                int count = Math.max(1, stack.getCount());
                for (int i = 0; i < count; i++) {
                    merged.add(stack.copyWithCount(1));
                }
                continue;
            }

            boolean found = false;
            for (ItemStack target : merged) {
                if (!target.isStackable()) {
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(target, stack)) {
                    target.grow(stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                merged.add(stack);
            }
        }

        ArrayList<ItemStack> normalized = new ArrayList<>(merged.size());
        for (ItemStack stack : merged) {
            int total = Math.max(1, stack.getCount());
            int max = Math.max(1, stack.getMaxStackSize());
            while (total > 0) {
                int part = Math.min(max, total);
                normalized.add(stack.copyWithCount(part));
                total -= part;
            }
        }

        normalized.sort(
                Comparator.comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                        .thenComparingInt(ItemStack::hashItemAndComponents)
                        .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed())
        );
        writeSorted(activeItems, size, normalized);
    }

    private static void sortDeepStorage(NonNullList<ItemStack> activeItems, int size) {
        ArrayList<ItemStack> merged = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = activeItems.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.isStackable()) {
                int count = Math.max(1, stack.getCount());
                for (int j = 0; j < count; j++) {
                    merged.add(DeepStorageStacks.copyWithoutDeepCount(stack, 1));
                }
                continue;
            }

            long count = DeepStorageStacks.getDeepCount(stack);
            if (count <= 0L) {
                count = stack.getCount();
            }
            boolean found = false;
            for (int targetIndex = 0; targetIndex < merged.size(); targetIndex++) {
                ItemStack target = merged.get(targetIndex);
                if (!target.isStackable() || !DeepStorageStacks.isSameItemSameComponentsIgnoringDeepCount(target, stack)) {
                    continue;
                }
                long targetCount = DeepStorageStacks.getDeepCount(target);
                if (targetCount <= 0L) {
                    targetCount = target.getCount();
                }
                merged.set(targetIndex, DeepStorageStacks.makeDeepMarker(target, DeepStorageStacks.saturatedAdd(targetCount, count)));
                found = true;
                break;
            }
            if (!found) {
                merged.add(DeepStorageStacks.makeDeepMarker(stack, count));
            }
        }

        merged.sort(
                Comparator.comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                        .thenComparingInt(stack -> ItemStack.hashItemAndComponents(DeepStorageStacks.copyWithoutDeepCount(stack, 1)))
                        .thenComparing(Comparator.comparingLong(ChestContentSorter::sortCount).reversed())
        );
        writeSorted(activeItems, size, merged);
    }

    private static void writeSorted(NonNullList<ItemStack> activeItems, int size, ArrayList<ItemStack> sorted) {
        int out = 0;
        for (; out < sorted.size() && out < size; out++) {
            activeItems.set(out, sorted.get(out));
        }
        for (; out < size; out++) {
            activeItems.set(out, ItemStack.EMPTY);
        }
    }

    private static long sortCount(ItemStack stack) {
        long deep = DeepStorageStacks.getDeepCount(stack);
        return deep > 0L ? deep : stack.getCount();
    }
}
