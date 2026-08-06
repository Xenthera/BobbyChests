package com.bobby.bobbychests.client.chest.screen.resource;

import com.bobby.bobbychests.chest.menu.resource.AbstractResourceChestMenu;
import com.bobby.bobbychests.chest.menu.resource.EnergyChestMenu;
import com.bobby.bobbychests.client.chest.screen.widget.EnergyGauge;
import com.bobby.bobbychests.client.chest.screen.widget.TankGauge;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class EnergyChestScreen extends AbstractResourceChestScreen<EnergyChestMenu> {

    private static final EnergyGauge GAUGE = new EnergyGauge(
            AbstractResourceChestMenu.gaugeX(),
            AbstractResourceChestMenu.gaugeY(),
            AbstractResourceChestMenu.GAUGE_WIDTH,
            AbstractResourceChestMenu.gaugeHeight());

    public EnergyChestScreen(EnergyChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void drawGauge(GuiGraphicsExtractor graphics, UiTheme theme) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        GAUGE.draw(graphics, theme, this.contents().energy(), this.menu.getCapacityFe());
        graphics.pose().popMatrix();
    }

    @Override
    protected Component readout() {
        return Component.translatable(
                "gui.bobbychests.energy.amount",
                TankGauge.format(this.contents().energy()),
                TankGauge.format(this.menu.getCapacityFe()));
    }

    @Override
    protected List<Component> gaugeTooltip(int mouseX, int mouseY) {
        if (!GAUGE.isMouseOver(mouseX - this.leftPos, mouseY - this.topPos)) {
            return List.of();
        }
        return GAUGE.tooltip(this.contents().energy(), this.menu.getCapacityFe());
    }
}
