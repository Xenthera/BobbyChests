package com.bobby.bobbychests.chest.menu;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.IMenuProviderExtension;

/**
 * Shared implementation for our tier-specific menu providers.
 *
 * <p>Writes the common open-menu payload: (pos, id, locked, ownerUuid, storage mode).</p>
 */
public abstract class AbstractTieredChestMenuProvider implements MenuProvider, IMenuProviderExtension {
    protected final Component title;
    protected final RoutedChestContainer container;
    protected final BlockPos pos;
    protected final AbstractTieredChestBlockEntity chest;

    protected AbstractTieredChestMenuProvider(Component title, RoutedChestContainer container, BlockPos pos, AbstractTieredChestBlockEntity chest) {
        this.title = title;
        this.container = container;
        this.pos = pos;
        this.chest = chest;
    }

    protected final ChestUpgradeContainer upgrades() {
        return new ChestUpgradeContainer(this.chest);
    }

    /** Whether this block entity's storage mode is {@link ChestStorageMode#GLOBAL}. */
    protected final boolean usesGlobalStorage() {
        return this.chest.getStorageMode() == ChestStorageMode.GLOBAL;
    }

    @Override
    public final Component getDisplayName() {
        return this.title;
    }

    @Override
    public final void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.chest.getTier().maxChannelId());
        buf.writeVarInt(this.chest.getGlobalStorageId());
        buf.writeBoolean(this.chest.isLocked());
        buf.writeUtf(this.chest.getOwnerUuid() == null ? "" : this.chest.getOwnerUuid().toString());
        buf.writeBoolean(this.usesGlobalStorage());
    }

    @Override
    public final AbstractContainerMenu createMenu(int syncID, Inventory playerInv, Player player) {
        return createMenuImpl(syncID, playerInv, player);
    }

    protected abstract AbstractContainerMenu createMenuImpl(int syncID, Inventory playerInv, Player player);
}

