package com.bobby.bobbychests.chest.menu.resource;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractTieredChestMenuProvider;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.chest.storage.ChestTransferContainer;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Opens the tank or energy-cell screen for a chest whose upgrade cards put it in that mode.
 *
 * <p>One provider for both, because the only difference is which menu gets constructed. The base
 * class still takes a {@link RoutedChestContainer} so the shared client payload writer is reused
 * unchanged, but the menu is handed the transfer slots instead: a fluid chest has no item storage
 * for a menu to show.
 */
public final class ResourceChestMenuProvider extends AbstractTieredChestMenuProvider {

    private final ChestResourceMode mode;

    public ResourceChestMenuProvider(
            Component title, RoutedChestContainer container, BlockPos pos,
            AbstractTieredChestBlockEntity chest, ChestResourceMode mode) {
        super(title, container, pos, chest);
        this.mode = mode;
    }

    @Override
    protected AbstractContainerMenu createMenuImpl(int syncID, Inventory playerInv, Player player) {
        Container transferSlots = new ChestTransferContainer(this.chest);
        if (this.mode == ChestResourceMode.ENERGY) {
            return new EnergyChestMenu(
                    syncID,
                    playerInv,
                    transferSlots,
                    this.upgrades(),
                    this.pos,
                    this.chest.getGlobalStorageId(),
                    this.chest.isLocked(),
                    this.chest.getOwnerUuid(),
                    this.usesGlobalStorage(),
                    this.chest.getTier().maxChannelId(),
                    this.chest.getTier());
        }
        return new FluidChestMenu(
                syncID,
                playerInv,
                transferSlots,
                this.upgrades(),
                this.pos,
                this.chest.getGlobalStorageId(),
                this.chest.isLocked(),
                this.chest.getOwnerUuid(),
                this.usesGlobalStorage(),
                this.chest.getTier().maxChannelId(),
                this.chest.getTier());
    }
}
