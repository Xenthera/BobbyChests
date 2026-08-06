package com.bobby.bobbychests.client.chest.screen.resource;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.resource.AbstractResourceChestMenu;
import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.client.chest.screen.AbstractChestScreen;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Shared chrome for the tank and energy-cell screens.
 *
 * <p>Everything structural — the tab strip, the lock and network tabs, the channel box, the tier
 * theming — comes from {@link AbstractChestScreen} unchanged. All these subclasses add is the gauge
 * where the slot grid would be, and its tooltip.
 */
public abstract class AbstractResourceChestScreen<M extends AbstractResourceChestMenu>
        extends AbstractChestScreen<M> {

    protected AbstractResourceChestScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    /** No slot grid to sort. */
    @Override
    protected boolean shouldShowSortButton() {
        return false;
    }

    /**
     * The live contents to draw.
     *
     * <p>Read straight off the block entity, which the server keeps current through its update tag,
     * rather than through menu data slots: the amounts run to millions and change many times a
     * second while a pipe is filling, which is exactly what the block entity's throttled broadcast
     * already handles.
     */
    protected ChestResourceContents contents() {
        AbstractTieredChestBlockEntity chest = this.chestBlockEntity();
        return chest == null ? EMPTY : chest.getActiveResources();
    }

    private static final ChestResourceContents EMPTY = new ChestResourceContents();

    protected AbstractTieredChestBlockEntity chestBlockEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.menu.getChestPos())
                instanceof AbstractTieredChestBlockEntity chest ? chest : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.drawThemedChestBackground(graphics, mouseX, mouseY, partialTick);
        this.drawGauge(graphics, this.uiTheme());
        this.drawReadout(graphics, this.uiTheme());
    }

    protected abstract void drawGauge(GuiGraphicsExtractor graphics, UiTheme theme);

    /** Amount over capacity, e.g. {@code 12,000 / 72,000 mB}. */
    protected abstract Component readout();

    /**
     * Draws the readout beside the gauge, always visible rather than only on hover.
     *
     * <p>Placed horizontally rather than under the gauge because the panel is only as tall as the
     * tier's item grid — one row for a dirt or wooden chest — and there is no vertical room to
     * spare. Truncated rather than allowed to run under the transfer slots.
     */
    private void drawReadout(GuiGraphicsExtractor graphics, UiTheme theme) {
        int x = this.leftPos + AbstractResourceChestMenu.readoutX();
        int y = this.topPos + AbstractResourceChestMenu.readoutY();
        int available = AbstractResourceChestMenu.readoutMaxX() - AbstractResourceChestMenu.readoutX();

        Component text = this.readout();
        if (this.font.width(text) > available) {
            graphics.text(this.font,
                    this.font.plainSubstrByWidth(text.getString(), available),
                    x, y, theme.labelPrimary(), false);
            return;
        }
        graphics.text(this.font, text, x, y, theme.labelPrimary(), false);
    }

    /** Tooltip lines when the cursor is over the gauge, or empty when it is not. */
    protected abstract List<Component> gaugeTooltip(int mouseX, int mouseY);

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> tooltip = this.gaugeTooltip(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
