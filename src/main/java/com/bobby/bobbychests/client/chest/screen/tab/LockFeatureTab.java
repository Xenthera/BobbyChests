package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbycore.client.gui.ExpandableTab;
import com.bobby.bobbycore.client.gui.font.BobbyFonts;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import com.bobby.bobbycore.client.gui.widget.UiCheckbox;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Lock upgrade feature tab: hosts the lock checkbox when open (amber tint).
 */
public final class LockFeatureTab extends ExpandableTab {
    public static final int TINT = 0xFFE8C872;
    public static final int CONTENT_TOP = AbstractChestMenu.UPGRADE_TAB_CONTENT_TOP;
    public static final int CONTENT_PAD = AbstractChestMenu.UPGRADE_TAB_PAD;
    private static final UiTheme TAB_THEME = FeatureTabChrome.themeFromTint(TINT);

    public LockFeatureTab() {
        super(
                BobbyFonts.translatable("gui.bobbychests.tab.lock"),
                96,
                CONTENT_TOP + UiCheckbox.TRACK_H + CONTENT_PAD);
        setKeepIconOnExpand(true);
        setTitleColor(FeatureTabChrome.titleColor(TINT));
    }

    public static UiTheme tabTheme() {
        return TAB_THEME;
    }

    /** Screen X for a widget of {@code width} centered in the open tab body. */
    public int contentWidgetX(int width) {
        return getContentOriginX() + Math.max(CONTENT_PAD, (getOpenWidth() - width) / 2);
    }

    public int contentWidgetY() {
        return getY() + CONTENT_TOP;
    }

    @Override
    protected void renderPanelBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        FeatureTabChrome.drawPanel(graphics, TAB_THEME, x, y, w, h);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        renderItemIcon(graphics, ModItems.LOCK_UPGRADE_CARD.get().getDefaultInstance(), x, y);
    }

    @Override
    protected void renderContent(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        // Lock toggle is a screen widget parked into this tab while open.
    }
}
