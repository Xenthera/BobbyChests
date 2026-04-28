package com.bobby.bobbychests.chest.block.diamond;

import com.bobby.bobbychests.chest.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.diamond.DiamondChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DiamondChestBlock extends AbstractTieredChestBlock {
    public DiamondChestBlock(Properties properties) {
        super(ModBlockEntities.DIAMOND_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.METAL).strength(2.5f));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.diamond_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        RoutedChestContainer container = new RoutedChestContainer(chest);
        return new DiamondChestMenuProvider(title, container, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiamondChestBlockEntity(pos, state);
    }
}

