package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbycore.client.gui.ExpandableTab;
import com.bobby.bobbycore.client.gui.font.BobbyFonts;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Networking upgrade feature tab: hosts the channel ID field when open (cyan tint).
 */
public final class NetworkFeatureTab extends ExpandableTab {
    public static final int TINT = 0xFF6EC6E8;
    public static final int CONTENT_TOP = AbstractChestMenu.UPGRADE_TAB_CONTENT_TOP;
    public static final int CONTENT_PAD = AbstractChestMenu.UPGRADE_TAB_PAD;
    public static final Component ID_LABEL = BobbyFonts.literal("ID:");
    private static final UiTheme TAB_THEME = FeatureTabChrome.themeFromTint(TINT);
    private static final int LABEL_GAP = 3;

    private final int idBoxHeight;

    public NetworkFeatureTab(int idBoxWidth, int idBoxHeight) {
        super(
                BobbyFonts.translatable("gui.bobbychests.tab.network"),
                openWidthFor(idBoxWidth),
                CONTENT_TOP + idBoxHeight + CONTENT_PAD);
        this.idBoxHeight = idBoxHeight;
        setKeepIconOnExpand(true);
        setTitleColor(FeatureTabChrome.titleColor(TINT));
    }

    private static int openWidthFor(int idBoxWidth) {
        int labelW = Minecraft.getInstance().font.width(ID_LABEL);
        return CONTENT_PAD + labelW + LABEL_GAP + idBoxWidth + CONTENT_PAD;
    }

    public static UiTheme tabTheme() {
        return TAB_THEME;
    }

    public int idLabelX() {
        return getContentOriginX() + CONTENT_PAD;
    }

    public int idBoxX() {
        return idLabelX() + Minecraft.getInstance().font.width(ID_LABEL) + LABEL_GAP;
    }

    public int idBoxY() {
        return getY() + CONTENT_TOP;
    }

    public int idLabelY() {
        return idBoxY() + (idBoxHeight - 8) / 2;
    }

    @Override
    protected void renderPanelBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        FeatureTabChrome.drawPanel(graphics, TAB_THEME, x, y, w, h);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        renderItemIcon(graphics, ModItems.NETWORKING_UPGRADE_CARD.get().getDefaultInstance(), x, y);
    }

    @Override
    protected void renderContent(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        graphics.text(
                Minecraft.getInstance().font,
                ID_LABEL,
                idLabelX(),
                idLabelY(),
                FeatureTabChrome.titleColor(TINT),
                false);
    }
}
