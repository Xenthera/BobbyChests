package com.bobby.bobbychests.client.chest.screen.diamond;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.client.chest.screen.AbstractChestScreen;
import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DiamondChestScreen extends AbstractChestScreen<DiamondChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_108.png");
    private static final Identifier BG_NO_ID = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_108_no_id.png");

    public DiamondChestScreen(DiamondChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTieredChestGuiBackground(graphics, mouseX, mouseY, partialTick, BG, BG_NO_ID, 512);
    }
}
