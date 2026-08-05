package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.network.SetUpgradeTabOpenPayload;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbycore.client.gui.ExpandableTab;
import com.bobby.bobbycore.client.gui.draw.SlotChrome;
import com.bobby.bobbycore.client.gui.font.BobbyFonts;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Upgrade-card inventory tab (mint tint). Only the slot strip, feature controls live in their own tabs.
 */
public final class UpgradeSlotsTab extends ExpandableTab {
    public static final int TINT = 0xFFA8E6A3;
    private static final int PAD = AbstractChestMenu.UPGRADE_TAB_PAD;
    private static final int SLOT = AbstractChestMenu.UPGRADE_SLOT_SIZE;
    private static final int CONTENT_TOP = AbstractChestMenu.UPGRADE_TAB_CONTENT_TOP;
    private static final UiTheme TAB_THEME = FeatureTabChrome.themeFromTint(TINT);

    private final AbstractChestMenu menu;

    public UpgradeSlotsTab(AbstractChestMenu menu) {
        super(
                BobbyFonts.translatable("gui.bobbychests.tab.upgrades"),
                PAD * 2 + SLOT,
                CONTENT_TOP + menu.getUpgradeSlotCount() * SLOT + PAD);
        this.menu = menu;
        setKeepIconOnExpand(true);
        setTitleColor(FeatureTabChrome.titleColor(TINT));
        setHandlers(this::onFullyOpen, this::onCloseStarted);
        menu.setUpgradeSlotsActive(false);
    }

    private void onFullyOpen() {
        setUpgradeTabOpen(true);
    }

    private void onCloseStarted() {
        setUpgradeTabOpen(false);
    }

    private void setUpgradeTabOpen(boolean open) {
        menu.setUpgradeSlotsActive(open);
        ClientPacketDistributor.sendToServer(new SetUpgradeTabOpenPayload(menu.getChestPos(), open));
    }

    @Override
    protected void renderPanelBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        FeatureTabChrome.drawPanel(graphics, TAB_THEME, x, y, w, h);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        renderItemIcon(graphics, ModItems.BLANK_UPGRADE_CARD.get().getDefaultInstance(), x, y);
    }

    @Override
    protected void renderContent(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        int count = menu.getUpgradeSlotCount();
        for (int i = 0; i < count; i++) {
            int px = x + PAD;
            int py = y + CONTENT_TOP + i * SLOT;
            SlotChrome.drawFrame(
                    graphics,
                    TAB_THEME,
                    UiTheme.SlotStyle.INVENTORY,
                    px - 1,
                    py - 1,
                    false,
                    true,
                    i == count - 1);
        }
    }
}
