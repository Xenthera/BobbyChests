package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestData;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

final class TieredChestOpenersCounter extends ContainerOpenersCounter {
    private final AbstractTieredChestBlockEntity chest;

    TieredChestOpenersCounter(AbstractTieredChestBlockEntity chest) {
        this.chest = chest;
    }

    @Override
    public boolean isOwnContainer(Player player) {
        if (!(player.containerMenu instanceof AbstractChestMenu menu)) {
            return false;
        }
        Container container = menu.getContainer();
        if (container == this.chest) {
            return true;
        }
        return container instanceof RoutedChestContainer routed && routed.getChest() == this.chest;
    }

    @Override
    protected void onOpen(Level level, BlockPos pos, BlockState state) {
        this.playChestSound(level, pos, state, true);
    }

    @Override
    protected void onClose(Level level, BlockPos pos, BlockState state) {
        if (this.chest.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey key = data.keyForChest(this.chest);
            if (data.getPublicOpenViewerCount(key) > 0) {
                return;
            }
        }
        this.playChestSound(level, pos, state, false);
    }

    @Override
    protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        if (this.chest.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey key = data.keyForChest(this.chest);
            int publicCount = data.getPublicOpenViewerCount(key);
            if (publicCount > 0) {
                this.chest.signalOpenCountFromCounter(level, pos, state, oldCount, publicCount);
                return;
            }
        }

        this.chest.signalOpenCountFromCounter(level, pos, state, oldCount, newCount);
        if (this.chest.getStorageMode() == ChestStorageMode.GLOBAL
                && level instanceof ServerLevel serverLevel
                && oldCount <= 0
                && newCount > 0) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey key = data.keyForChest(this.chest);
            if (data.getPublicOpenViewerCount(key) <= 0) {
                data.toggleObserverSignal(serverLevel, key);
            }
        }
    }

    private void playChestSound(Level level, BlockPos pos, BlockState state, boolean open) {
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return;
        }
        float pitch = level.getRandom().nextFloat() * 0.1F + 0.9F;
        level.playSound(
                null,
                pos,
                open ? chestBlock.getOpenChestSound() : chestBlock.getCloseChestSound(),
                SoundSource.BLOCKS,
                0.5F,
                pitch);
    }
}
