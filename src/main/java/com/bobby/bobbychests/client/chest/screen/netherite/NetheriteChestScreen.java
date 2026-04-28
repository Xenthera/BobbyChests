package com.bobby.bobbychests.client.chest.screen.netherite;

import com.bobby.bobbychests.client.chest.screen.AbstractScrollableChestScreen;
import com.bobby.bobbychests.client.chest.screen.ScrollableChestGuiAssets;
import com.bobby.bobbychests.chest.menu.netherite.NetheriteChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class NetheriteChestScreen extends AbstractScrollableChestScreen<NetheriteChestMenu> {
    public NetheriteChestScreen(NetheriteChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, ScrollableChestGuiAssets.netheriteStyle108());
    }
}
