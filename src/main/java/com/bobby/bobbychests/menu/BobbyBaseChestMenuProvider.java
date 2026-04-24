package com.bobby.bobbychests.menu;

import com.bobby.bobbychests.blockentity.BobbyBaseChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalBobbyBaseChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.IMenuProviderExtension;

/**
 * Server-side menu provider for {@link BobbyBaseChestMenu}.
 *
 * <p>Important: this should only be created/used on the server; the client gets the
 * data it needs via {@link #writeClientSideData(AbstractContainerMenu, RegistryFriendlyByteBuf)}.</p>
 */
public final class BobbyBaseChestMenuProvider implements MenuProvider, IMenuProviderExtension {

    private final Component title;
    private final GlobalBobbyBaseChestContainer container;
    private final BlockPos pos;
    private final BobbyBaseChestBlockEntity chest;

    public BobbyBaseChestMenuProvider(Component title, GlobalBobbyBaseChestContainer container, BlockPos pos, BobbyBaseChestBlockEntity chest) {
        this.title = title;
        this.container = container;
        this.pos = pos;
        this.chest = chest;
    }

    @Override
    public Component getDisplayName() {
        return this.title;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncID, Inventory playerInv, Player player) {
        return new BobbyBaseChestMenu(
                syncID,
                playerInv,
                this.container,
                this.pos,
                this.chest.getGlobalStorageId(),
                this.chest.isLocked(),
                this.chest.getOwnerUuid()
        );
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.chest.getGlobalStorageId());
        buf.writeBoolean(this.chest.isLocked());
        buf.writeUtf(this.chest.getOwnerUuid() == null ? "" : this.chest.getOwnerUuid().toString());
    }
}

