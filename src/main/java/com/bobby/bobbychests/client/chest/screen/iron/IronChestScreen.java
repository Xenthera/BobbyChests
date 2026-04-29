package com.bobby.bobbychests.client.chest.screen.iron;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.client.chest.screen.AbstractChestScreen;
import com.bobby.bobbychests.chest.menu.iron.IronChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class IronChestScreen extends AbstractChestScreen<IronChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_27.png");
    private static final Identifier BG_NO_ID = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_27_no_id.png");

    public IronChestScreen(IronChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTieredChestGuiBackground(graphics, mouseX, mouseY, partialTick, BG, BG_NO_ID, 256);
    }
}
