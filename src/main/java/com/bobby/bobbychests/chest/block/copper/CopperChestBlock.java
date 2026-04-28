package com.bobby.bobbychests.chest.block.copper;

import com.bobby.bobbychests.chest.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.copper.CopperChestBlockEntity;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import com.bobby.bobbychests.chest.menu.copper.CopperChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopperChestBlock extends AbstractTieredChestBlock {
    public CopperChestBlock(Properties properties) {
        super(ModBlockEntities.COPPER_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.COPPER).strength(1.5f));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.copper_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        RoutedChestContainer container = new RoutedChestContainer(chest);
        return new CopperChestMenuProvider(title, container, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperChestBlockEntity(pos, state);
    }
}

