package com.bobby.bobbychests.client.chest.screen.diamond;

import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenu;
import com.bobby.bobbychests.client.chest.screen.AbstractScrollableChestScreen;
import com.bobby.bobbychests.client.chest.screen.ScrollableChestGuiAssets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DiamondChestScreen extends AbstractScrollableChestScreen<DiamondChestMenu> {
    public DiamondChestScreen(DiamondChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, ScrollableChestGuiAssets.diamondStyle108());
    }
}
