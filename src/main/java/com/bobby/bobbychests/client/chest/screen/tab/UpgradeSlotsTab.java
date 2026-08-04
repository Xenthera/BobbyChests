package com.bobby.bobbychests.client.chest.screen.tab;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.network.SetUpgradeTabOpenPayload;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbycore.client.gui.ExpandableTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Right-edge upgrades tab. Upgrade slots are active only when fully open (synced to server).
 * Slot x/y are fixed at menu construction (final on MC 26); visibility uses {@code isActive}.
 */
public final class UpgradeSlotsTab extends ExpandableTab {
    private static final int PAD = AbstractChestMenu.UPGRADE_TAB_PAD;
    private static final int SLOT = AbstractChestMenu.UPGRADE_SLOT_SIZE;
    private static final int CONTENT_TOP = AbstractChestMenu.UPGRADE_TAB_CONTENT_TOP;
    /** Soft mint multiply-tint for the creative-style tab panel and slots. */
    private static final int PANEL_TINT = 0xFFA8E6A3;
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private final AbstractChestMenu menu;

    public UpgradeSlotsTab(AbstractChestMenu menu) {
        super(
                Component.translatable("gui.bobbychests.tab.upgrades"),
                PAD * 2 + SLOT,
                CONTENT_TOP + menu.getUpgradeSlotCount() * SLOT + PAD);
        this.menu = menu;
        setKeepIconOnExpand(true);
        setTitleColor(0xFFFFFFFF);
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
        ExpandableTab.DefaultPanelStyle.blit(graphics, x, y, w, h, PANEL_TINT);
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        ItemStack icon = ModItems.BLANK_UPGRADE_CARD.get().getDefaultInstance();
        renderItemIcon(graphics, icon, x, y);
    }

    @Override
    protected void renderContent(
            GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < menu.getUpgradeSlotCount(); i++) {
            // Vanilla: slot.x/y is the 16x16 item origin; container/slot.png is the 18x18 hole at (x-1,y-1).
            int px = x + PAD - 1;
            int py = y + CONTENT_TOP + i * SLOT - 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, px, py, SLOT, SLOT, PANEL_TINT);
        }
    }
}
