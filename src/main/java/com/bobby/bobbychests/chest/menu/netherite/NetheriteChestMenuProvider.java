package com.bobby.bobbychests.chest.menu.netherite;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractTieredChestMenuProvider;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class NetheriteChestMenuProvider extends AbstractTieredChestMenuProvider {
    public NetheriteChestMenuProvider(Component title, RoutedChestContainer container, BlockPos pos, AbstractTieredChestBlockEntity chest) {
        super(title, container, pos, chest);
    }

    @Override
    protected AbstractContainerMenu createMenuImpl(int syncID, Inventory playerInv, Player player) {
        return new NetheriteChestMenu(
                syncID,
                playerInv,
                this.container,
                this.upgrades(),
                this.pos,
                this.chest.getGlobalStorageId(),
                this.chest.isLocked(),
                this.chest.getOwnerUuid(),
                this.usesGlobalStorage(),
                this.chest.getTier().maxChannelId()
        );
    }
}
