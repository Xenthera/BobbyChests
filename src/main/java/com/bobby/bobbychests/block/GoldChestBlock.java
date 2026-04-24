package com.bobby.bobbychests.block;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.GoldChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.bobby.bobbychests.menu.GoldChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GoldChestBlock extends AbstractTieredChestBlock {
    public GoldChestBlock(Properties properties) {
        super(ModBlockEntities.GOLD_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties.sound(SoundType.METAL).strength(2.0f));
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.gold_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        GlobalTieredChestData global = GlobalTieredChestData.get(serverLevel);
        GlobalTieredChestContainer globalContainer = new GlobalTieredChestContainer(global, chest);
        return new GoldChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldChestBlockEntity(pos, state);
    }
}

