package com.bobby.bobbychests.client.chest.screen.diamond;

import com.bobby.bobbychests.client.chest.screen.AbstractChestScreen;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DiamondChestScreen extends AbstractChestScreen<DiamondChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_108.png");
    private static final int ID_BOX_W = 62;

    public DiamondChestScreen(DiamondChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected int idBoxX() {
        int rightPad = 15;
        return this.leftPos + this.imageWidth - ID_BOX_W - rightPad;
    }

    @Override
    protected int idBoxY() {
        return this.topPos + 5;
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
                512, 512
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
    }
}

