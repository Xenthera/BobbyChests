package com.bobby.bobbychests.block.wooden;

import com.bobby.bobbychests.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.wooden.WoodenChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.bobby.bobbychests.menu.wooden.WoodenChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WoodenChestBlock extends AbstractTieredChestBlock {

    public WoodenChestBlock(Properties properties) {
        super(ModBlockEntities.WOODEN_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.WOOD));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.wooden_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        GlobalTieredChestData global = GlobalTieredChestData.get(serverLevel);
        GlobalTieredChestContainer globalContainer = new GlobalTieredChestContainer(global, chest);
        return new WoodenChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodenChestBlockEntity(pos, state);
    }
}

