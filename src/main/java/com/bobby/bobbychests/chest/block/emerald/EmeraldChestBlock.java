package com.bobby.bobbychests.chest.block.emerald;

import com.bobby.bobbychests.chest.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.emerald.EmeraldChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestContainer;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestData;
import com.bobby.bobbychests.chest.menu.emerald.EmeraldChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EmeraldChestBlock extends AbstractTieredChestBlock {
    public EmeraldChestBlock(Properties properties) {
        super(ModBlockEntities.EMERALD_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.METAL).strength(3.0f));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.emerald_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        GlobalTieredChestData global = GlobalTieredChestData.get(serverLevel);
        GlobalTieredChestContainer globalContainer = new GlobalTieredChestContainer(global, chest);
        return new EmeraldChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmeraldChestBlockEntity(pos, state);
    }
}
