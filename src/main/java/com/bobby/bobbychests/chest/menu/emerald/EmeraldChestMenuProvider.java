package com.bobby.bobbychests.chest.menu.emerald;

import com.bobby.bobbychests.registry.ModMenus;
import com.bobby.bobbychests.chest.menu.AbstractTieredChestMenuProvider;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class EmeraldChestMenuProvider extends AbstractTieredChestMenuProvider {
    public EmeraldChestMenuProvider(Component title, GlobalTieredChestContainer container, BlockPos pos, AbstractTieredChestBlockEntity chest) {
        super(title, container, pos, chest);
    }

    @Override
    protected AbstractContainerMenu createMenuImpl(int syncID, Inventory playerInv, Player player) {
        return new EmeraldChestMenu(
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
