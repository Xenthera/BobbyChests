package com.bobby.bobbychests.client.chest.screen.dirt;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.client.chest.screen.AbstractChestScreen;
import com.bobby.bobbychests.chest.menu.dirt.DirtChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DirtChestScreen extends AbstractChestScreen<DirtChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_1.png");
    private static final Identifier BG_NO_ID = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_1_no_id.png");

    public DirtChestScreen(DirtChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean shouldShowSortButton() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTieredChestGuiBackground(graphics, mouseX, mouseY, partialTick, BG, BG_NO_ID, 256);
    }
}
