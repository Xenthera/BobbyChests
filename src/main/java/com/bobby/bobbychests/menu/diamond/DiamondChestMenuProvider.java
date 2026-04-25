package com.bobby.bobbychests.menu.diamond;

import com.bobby.bobbychests.menu.ModMenus;
import com.bobby.bobbychests.menu.AbstractTieredChestMenuProvider;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class DiamondChestMenuProvider extends AbstractTieredChestMenuProvider {
    public DiamondChestMenuProvider(Component title, GlobalTieredChestContainer container, BlockPos pos, AbstractTieredChestBlockEntity chest) {
        super(title, container, pos, chest);
    }

    @Override
    protected AbstractContainerMenu createMenuImpl(int syncID, Inventory playerInv, Player player) {
        return new DiamondChestMenu(
                syncID,
                playerInv,
                this.container,
                this.pos,
                this.chest.getGlobalStorageId(),
                this.chest.isLocked(),
                this.chest.getOwnerUuid(),
                this.chest.getTier().maxChannelId()
        );
    }
}

