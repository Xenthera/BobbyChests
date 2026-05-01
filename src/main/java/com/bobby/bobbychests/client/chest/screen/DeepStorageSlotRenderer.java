package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DeepStorageSlotRenderer {
    private DeepStorageSlotRenderer() {
    }

    static void renderSlotCount(GuiGraphicsExtractor graphics, Font font, AbstractChestMenu menu, Slot slot) {
        long deep = getDeepCountForSlot(menu, slot);
        if (deep <= 0L) {
            return;
        }
        String label = abbreviateCount(deep);
        float scale = labelScale(label);
        int color = 0xFFFFFFFF;
        int labelX = Math.round(slot.x + 17 - font.width(label) * scale);
        int labelY = slot.y + labelYOffset(label, scale);
        graphics.pose().pushMatrix();
        graphics.pose().translate(labelX, labelY);
        graphics.pose().scale(scale, scale);
        graphics.text(font, label, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    static List<Component> appendStoredCountTooltip(ItemStack stack, List<Component> tooltip) {
        long deep = DeepStorageStacks.getDeepCount(stack);
        if (deep <= 0L) {
            return tooltip;
        }
        ArrayList<Component> withCount = new ArrayList<>(tooltip);
        String fullCount = NumberFormat.getIntegerInstance(Locale.US).format(deep);
        withCount.add(Component.literal("Stored: " + fullCount));
        return withCount;
    }

    private static long getDeepCountForSlot(AbstractChestMenu menu, Slot slot) {
        if (!menu.hasDeepStorageUpgradeInstalled()) {
            return 0L;
        }
        if (slot == null || slot.index >= menu.getChestSlotCount() || !slot.hasItem()) {
            return 0L;
        }
        return DeepStorageStacks.getDeepCount(slot.getItem());
    }

    private static String abbreviateCount(long value) {
        if (value < 0) {
            return "0";
        }
        if (value < 1000L) {
            return Long.toString(value);
        }
        final String[] suffixes = {"k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
        double v = (double) value;
        int idx = -1;
        while (v >= 1000.0 && idx + 1 < suffixes.length) {
            v /= 1000.0;
            idx++;
        }
        if (idx < 0) {
            return Long.toString(value);
        }
        String s = v >= 100.0 ? String.format("%.0f", v) : (v >= 10.0 ? String.format("%.1f", v) : String.format("%.2f", v));
        if (s.endsWith(".00")) {
            s = s.substring(0, s.length() - 3);
        } else if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s + suffixes[idx];
    }

    private static float labelScale(String label) {
        if (label.length() >= 5) {
            return 0.55F;
        }
        if (label.length() >= 3) {
            return 0.70F;
        }
        return 1.0F;
    }

    private static int labelYOffset(String label, float scale) {
        if (label.length() >= 5) {
            return 11;
        }
        return Math.round(9.0F + (1.0F - scale) * 8.0F);
    }
}
