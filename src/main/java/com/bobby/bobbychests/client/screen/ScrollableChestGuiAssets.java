package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.resources.Identifier;

/**
 * Background + vertical 3-slice scrollbar textures for {@link AbstractScrollableChestScreen}.
 */
public record ScrollableChestGuiAssets(
        Identifier background,
        Identifier scrollTrackTop,
        Identifier scrollTrackCenter,
        Identifier scrollTrackBottom,
        Identifier scrollHandleTop,
        Identifier scrollHandleCenter,
        Identifier scrollHandleBottom
) {
    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath(BobbyChests.MODID, path);
    }

    /** 18×6 viewport over {@code bobby_base_chest_108.png} with emerald scrollbar art. */
    public static ScrollableChestGuiAssets emeraldStyle108() {
        return new ScrollableChestGuiAssets(
                tex("textures/gui/bobby_base_chest_108.png"),
                tex("textures/gui/scroll/emerald_scroll_track_top.png"),
                tex("textures/gui/scroll/emerald_scroll_track_center.png"),
                tex("textures/gui/scroll/emerald_scroll_track_bottom.png"),
                tex("textures/gui/scroll/emerald_scroll_handle_top.png"),
                tex("textures/gui/scroll/emerald_scroll_handle_center.png"),
                tex("textures/gui/scroll/emerald_scroll_handle_bottom.png")
        );
    }

    /** Netherite reuses emerald scrollbar assets until dedicated art exists. */
    public static ScrollableChestGuiAssets netheriteStyle108() {
        return emeraldStyle108();
    }
}
