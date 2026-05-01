package com.bobby.bobbychests.chest.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DeepStorageStacks {
    private static final String TAG_DEEP_COUNT = "bobbychests:deep_count";

    private DeepStorageStacks() {
    }

    public static long getDeepCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        CompoundTag tag = data.copyTag();
        return tag.getLong(TAG_DEEP_COUNT).orElse(0L);
    }

    public static void setDeepCount(ItemStack stack, long count) {
        if (stack.isEmpty()) {
            return;
        }
        if (count <= 0L) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                return;
            }
            CompoundTag tag = data.copyTag();
            tag.remove(TAG_DEEP_COUNT);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        tag.putLong(TAG_DEEP_COUNT, count);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack makeDeepMarker(ItemStack base, long deepCount) {
        if (base.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long clamped = Math.max(0L, deepCount);
        ItemStack marker = base.copyWithCount(1);
        setDeepCount(marker, clamped);
        return marker;
    }

    public static ItemStack copyWithoutDeepCount(ItemStack base, int count) {
        if (base.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = base.copyWithCount(count);
        setDeepCount(out, 0L);
        return out;
    }

    public static boolean isSameItemSameComponentsIgnoringDeepCount(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() && right.isEmpty();
        }
        ItemStack cleanLeft = copyWithoutDeepCount(left, 1);
        ItemStack cleanRight = copyWithoutDeepCount(right, 1);
        return ItemStack.isSameItemSameComponents(cleanLeft, cleanRight);
    }

    public static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }
}
