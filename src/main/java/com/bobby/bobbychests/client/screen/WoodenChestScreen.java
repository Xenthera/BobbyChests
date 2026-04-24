package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.menu.WoodenChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class WoodenChestScreen extends AbstractChestScreen<WoodenChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_9.png");

    public WoodenChestScreen(WoodenChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BG,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                256, 256
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
    }
}

