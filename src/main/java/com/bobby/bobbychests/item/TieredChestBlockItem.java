package com.bobby.bobbychests.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adds a small preview of retained contents when this chest item carries block-entity data.
 */
public final class TieredChestBlockItem extends BlockItem {
    private static final int TOOLTIP_PREVIEW_LIMIT = 3;

    public TieredChestBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext ctx,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, tooltip, flag);

        TypedEntityData<?> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData == null || ctx == null || ctx.registries() == null) {
            return;
        }

        CompoundTag tag = beData.copyTagWithoutId();
        ListTag itemsTag = tag.getList("Items").orElse(null);
        if (itemsTag == null || itemsTag.isEmpty()) {
            return;
        }

        int maxSlotExclusive = 0;
        for (int i = 0; i < itemsTag.size(); i++) {
            CompoundTag entry = itemsTag.getCompound(i).orElse(null);
            if (entry == null) {
                continue;
            }
            int slot = entry.getByte("Slot").orElse((byte) 0) & 0xFF;
            maxSlotExclusive = Math.max(maxSlotExclusive, slot + 1);
        }
        if (maxSlotExclusive <= 0) {
            return;
        }

        var items = net.minecraft.core.NonNullList.withSize(maxSlotExclusive, ItemStack.EMPTY);
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, ctx.registries(), tag);
        ContainerHelper.loadAllItems(input, items);

        int shown = 0;
        int nonEmpty = 0;
        for (ItemStack s : items) {
            if (s.isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (shown < TOOLTIP_PREVIEW_LIMIT) {
                tooltip.accept(Component.literal("- ")
                        .append(s.getHoverName())
                        .append(Component.literal(" x" + s.getCount())));
                shown++;
            }
        }
        int remaining = nonEmpty - shown;
        if (remaining > 0) {
            tooltip.accept(Component.literal("...and " + remaining + " more"));
        }
    }
}

