package com.bobby.bobbychests.client.chest.screen.resource;

import com.bobby.bobbychests.chest.menu.resource.AbstractResourceChestMenu;
import com.bobby.bobbychests.chest.menu.resource.FluidChestMenu;
import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.client.chest.screen.widget.TankGauge;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class FluidChestScreen extends AbstractResourceChestScreen<FluidChestMenu> {

    private static final TankGauge GAUGE = new TankGauge(
            AbstractResourceChestMenu.gaugeX(),
            AbstractResourceChestMenu.gaugeY(),
            AbstractResourceChestMenu.GAUGE_WIDTH,
            AbstractResourceChestMenu.gaugeHeight());

    public FluidChestScreen(FluidChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void drawGauge(GuiGraphicsExtractor graphics, UiTheme theme) {
        ChestResourceContents contents = this.contents();
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        GAUGE.draw(graphics, theme, contents.fluid(), contents.fluidAmount(), this.menu.getCapacityMb());
        graphics.pose().popMatrix();
    }

    @Override
    protected Component readout() {
        ChestResourceContents contents = this.contents();
        return Component.translatable(
                "gui.bobbychests.tank.amount",
                TankGauge.format(contents.fluidAmount()),
                TankGauge.format(this.menu.getCapacityMb()));
    }

    @Override
    protected List<Component> gaugeTooltip(int mouseX, int mouseY) {
        if (!GAUGE.isMouseOver(mouseX - this.leftPos, mouseY - this.topPos)) {
            return List.of();
        }
        ChestResourceContents contents = this.contents();
        return GAUGE.tooltip(contents.fluid(), contents.fluidAmount(), this.menu.getCapacityMb());
    }
}
