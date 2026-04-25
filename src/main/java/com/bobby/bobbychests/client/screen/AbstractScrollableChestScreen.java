package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.network.SetScrollableChestScrollPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Screen for {@link AbstractScrollableChestMenu}: syncs scroll reset when channel / lock / owner changes on the
 * block entity, and sends row scroll to the server.
 */
public abstract class AbstractScrollableChestScreen<M extends AbstractScrollableChestMenu> extends AbstractChestScreen<M> {
    private static final int SYNCED_CHANNEL_UNSET = Integer.MIN_VALUE;
    private int syncedChannelId = SYNCED_CHANNEL_UNSET;
    private boolean syncedLocked;
    private @Nullable UUID syncedOwnerUuid;

    protected AbstractScrollableChestScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void containerTick() {
        this.tickScrollFromSyncedBlockEntity();
        super.containerTick();
    }

    @Override
    protected void resetScrollMenuOnStorageKeyChange() {
        this.menu.onGlobalStorageContextChanged();
    }

    /**
     * When the block entity syncs a new channel / lock / owner (another player, commands, etc.), reset scroll so
     * window slots map into the correct backing list.
     */
    private void tickScrollFromSyncedBlockEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        if (!(this.minecraft.level.getBlockEntity(this.menu.getChestPos()) instanceof AbstractTieredChestBlockEntity be)) {
            return;
        }
        int channel = be.getGlobalStorageId();
        boolean locked = be.isLocked();
        UUID owner = be.getOwnerUuid();
        if (this.syncedChannelId == SYNCED_CHANNEL_UNSET) {
            this.syncedChannelId = channel;
            this.syncedLocked = locked;
            this.syncedOwnerUuid = owner;
            return;
        }
        if (channel != this.syncedChannelId
                || locked != this.syncedLocked
                || !Objects.equals(owner, this.syncedOwnerUuid)) {
            this.menu.onGlobalStorageContextChanged();
            this.syncedChannelId = channel;
            this.syncedLocked = locked;
            this.syncedOwnerUuid = owner;
        }
    }

    protected final void sendScrollRowsToServer() {
        ClientPacketDistributor.sendToServer(
                new SetScrollableChestScrollPayload(this.menu.getChestPos(), this.menu.getScrollRows())
        );
    }

    /**
     * Call after {@link AbstractScrollableChestMenu#setScrollRows(int)} or {@link AbstractScrollableChestMenu#applyScrollDelta(int)}
     * on the client when the value changed and should be persisted.
     */
    protected final void sendScrollRowsToServerAfterMenuChange(int previousScrollRows) {
        if (this.menu.getScrollRows() != previousScrollRows) {
            this.sendScrollRowsToServer();
        }
    }
}
