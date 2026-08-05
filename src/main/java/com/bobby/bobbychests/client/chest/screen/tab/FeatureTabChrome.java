package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbycore.client.gui.draw.UiDraw;
import com.bobby.bobbycore.client.gui.theme.BobbyThemes;
import com.bobby.bobbycore.client.gui.theme.UiColor;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared tinted panel + slot theme for chest feature tabs (lock / network / upgrades).
 */
final class FeatureTabChrome {
    private FeatureTabChrome() {
    }

    static UiTheme themeFromTint(int tintRgb) {
        int tint = tintRgb | 0xFF000000;
        int fill = UiColor.darken(tint, 0.18F) | 0xFF000000;
        int header = UiColor.raise(tint, 0.06F) | 0xFF000000;
        int border = UiColor.darken(tint, 0.42F) | 0xFF000000;
        int well = UiColor.darken(tint, 0.32F) | 0xFF000000;
        int slotBorder = UiColor.darken(tint, 0.38F) | 0xFF000000;
        int slotHover = UiColor.darken(tint, 0.22F) | 0xFF000000;
        return BobbyThemes.BOBBY_DARK
                .withPanelColors(fill, header, border)
                .withSlotColors(well, slotBorder, slotHover);
    }

    /** Dark title colour that stays readable on a light tinted header. */
    static int titleColor(int tintRgb) {
        return UiColor.darken(tintRgb | 0xFF000000, 0.72F) | 0xFF000000;
    }

    static void drawPanel(GuiGraphicsExtractor graphics, UiTheme theme, int x, int y, int w, int h) {
        UiDraw.fill(graphics, x, y, w, h, theme.panelFill());
        int headerH = Math.min(24, h);
        UiDraw.fill(graphics, x, y, w, headerH, theme.panelHeader());
        UiDraw.border(graphics, x, y, w, h, theme.panelBorder(), 1);
    }
}
