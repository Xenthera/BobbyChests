package com.bobby.bobbychests.block.netherite;

import com.bobby.bobbychests.block.AbstractTieredChestBlock;
import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.blockentity.netherite.NetheriteChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.bobby.bobbychests.menu.netherite.NetheriteChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class NetheriteChestBlock extends AbstractTieredChestBlock {
    public NetheriteChestBlock(Block.Properties properties) {
        super(
                ModBlockEntities.NETHERITE_CHEST::get,
                SoundEvents.CHEST_OPEN,
                SoundEvents.CHEST_CLOSE,
                properties.sound(SoundType.NETHERITE_BLOCK).strength(4.0f)
        );
    }

    @Override
    protected String titleKey() {
        return "container.bobbychests.netherite_chest";
    }

    @Override
    protected MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title) {
        GlobalTieredChestData global = GlobalTieredChestData.get(serverLevel);
        GlobalTieredChestContainer globalContainer = new GlobalTieredChestContainer(global, chest);
        return new NetheriteChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetheriteChestBlockEntity(pos, state);
    }
}
