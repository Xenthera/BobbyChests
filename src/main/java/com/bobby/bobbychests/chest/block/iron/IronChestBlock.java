package com.bobby.bobbychests.chest.block.iron;

import com.bobby.bobbychests.chest.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.iron.IronChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import com.bobby.bobbychests.chest.menu.iron.IronChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IronChestBlock extends AbstractTieredChestBlock {
    public IronChestBlock(Properties properties) {
        super(ModBlockEntities.IRON_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.METAL).strength(2.0f));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.iron_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        RoutedChestContainer container = new RoutedChestContainer(chest);
        return new IronChestMenuProvider(title, container, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronChestBlockEntity(pos, state);
    }
}

