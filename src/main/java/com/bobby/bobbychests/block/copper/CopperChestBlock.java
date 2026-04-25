package com.bobby.bobbychests.block.copper;

import com.bobby.bobbychests.block.AbstractTieredChestBlock;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.copper.CopperChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.bobby.bobbychests.menu.copper.CopperChestMenuProvider;
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
        GlobalTieredChestData global = GlobalTieredChestData.get(serverLevel);
        GlobalTieredChestContainer globalContainer = new GlobalTieredChestContainer(global, chest);
        return new CopperChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperChestBlockEntity(pos, state);
    }
}

