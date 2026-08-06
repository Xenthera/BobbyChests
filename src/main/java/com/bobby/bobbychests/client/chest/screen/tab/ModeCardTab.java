package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.network.SetModeTabOpenPayload;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbycore.client.gui.ExpandableTab;
import com.bobby.bobbycore.client.gui.draw.SlotChrome;
import com.bobby.bobbycore.client.gui.font.BobbyFonts;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The mode-card tab: one slot deciding whether the chest stores items, fluid, or FE.
 *
 * <p>Its own tab, and its own slot, so switching a chest to a tank costs no upgrade capacity. A
 * dirt chest has a single upgrade slot, and spending it on being a tank rather than on an actual
 * upgrade was a poor trade to force on anyone.
 *
 * <p>Blue tint, to read as distinct from the mint upgrades tab sitting above it.
 */
public final class ModeCardTab extends ExpandableTab {
    public static final int TINT = 0xFF8FC2E8;
    private static final int PAD = AbstractChestMenu.UPGRADE_TAB_PAD;
    private static final int SLOT = AbstractChestMenu.UPGRADE_SLOT_SIZE;
    private static final int CONTENT_TOP = AbstractChestMenu.UPGRADE_TAB_CONTENT_TOP;
    private static final UiTheme TAB_THEME = FeatureTabChrome.themeFromTint(TINT);

    private final AbstractChestMenu menu;

    public ModeCardTab(AbstractChestMenu menu) {
        super(
                BobbyFonts.translatable("gui.bobbychests.tab.mode"),
                PAD * 2 + SLOT,
                CONTENT_TOP + SLOT + PAD);
        this.menu = menu;
        setKeepIconOnExpand(true);
        setTitleColor(FeatureTabChrome.titleColor(TINT));
        setHandlers(this::onFullyOpen, this::onCloseStarted);
        menu.setModeSlotActive(false);
    }

    private void onFullyOpen() {
        setModeTabOpen(true);
    }

    private void onCloseStarted() {
        setModeTabOpen(false);
    }

    private void setModeTabOpen(boolean open) {
        menu.setModeSlotActive(open);
        ClientPacketDistributor.sendToServer(new SetModeTabOpenPayload(menu.getChestPos(), open));
    }

    @Override
    protected void renderPanelBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        FeatureTabChrome.drawPanel(graphics, TAB_THEME, x, y, w, h);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        renderItemIcon(graphics, ModItems.FLUID_UPGRADE_CARD.get().getDefaultInstance(), x, y);
    }

    @Override
    protected void renderContent(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        SlotChrome.drawFrame(
                graphics,
                TAB_THEME,
                UiTheme.SlotStyle.INVENTORY,
                x + PAD - 1,
                y + CONTENT_TOP - 1,
                false,
                true,
                true);
    }
}
