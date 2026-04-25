package com.bobby.bobbychests.client.screen.emerald;

import com.bobby.bobbychests.client.screen.AbstractScrollableChestScreen;
import com.bobby.bobbychests.client.screen.ScrollableChestGuiAssets;
import com.bobby.bobbychests.menu.emerald.EmeraldChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EmeraldChestScreen extends AbstractScrollableChestScreen<EmeraldChestMenu> {
    public EmeraldChestScreen(EmeraldChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, ScrollableChestGuiAssets.emeraldStyle108());
    }
}
