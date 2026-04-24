package com.bobby.bobbychests.block;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public abstract class AbstractTieredChestBlock extends ChestBlock {
    /**
     * Hidden state used to force a blockstate change on open/close so observers can detect it.
     * This does not affect rendering (both variants map to the same model in the blockstate json).
     */
    public static final BooleanProperty OBSERVER_OPEN = BooleanProperty.create("observer_open");

    protected AbstractTieredChestBlock(Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType, SoundEvent openSound, SoundEvent closeSound, Properties properties) {
        super(blockEntityType, openSound, closeSound, properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TYPE, ChestType.SINGLE)
                .setValue(OBSERVER_OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OBSERVER_OPEN);
    }

    protected abstract String titleKey();

    protected abstract MenuProvider createMenuProvider(ServerLevel serverLevel, BlockPos pos, AbstractTieredChestBlockEntity chest, Component title);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // Ender-chest-like portal particles.
        for (int i = 0; i < 3; i++) {
            int xOffset = random.nextInt(2) * 2 - 1;
            int zOffset = random.nextInt(2) * 2 - 1;
            double x = pos.getX() + 0.5 + 0.25 * xOffset;
            double y = pos.getY() + random.nextFloat();
            double z = pos.getZ() + 0.5 + 0.25 * zOffset;
            double vx = random.nextFloat() * xOffset;
            double vy = (random.nextFloat() - 0.5) * 0.125;
            double vz = random.nextFloat() * zOffset;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        // Whatever logic happening in vanilla here prevents using if chest is blocked from above.
        if (super.getMenuProvider(state, level, pos) == null) return null;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractTieredChestBlockEntity chest)) return null;

        Component title = Component.translatable(titleKey());

        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return createMenuProvider(serverLevel, pos, chest, title);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            BlockEntity be = serverLevel.getBlockEntity(pos);
            if (be instanceof AbstractTieredChestBlockEntity chest) {
                if (!chest.canPlayerOpen(player)) {
                    player.sendSystemMessage(Component.literal("This chest is locked.").withColor(ARGB.color(255, 255, 64, 64)));
                    // Consume so the held item doesn't get a chance to place.
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            BlockEntity be = serverLevel.getBlockEntity(pos);
            if (be instanceof AbstractTieredChestBlockEntity chest) {
                if (!chest.canPlayerOpen(player)) {
                    player.sendSystemMessage(Component.literal("This chest is locked.").withColor(ARGB.color(255, 255, 64, 64)));
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        // We never allow tiered chests to form double-chests.
        if (state.hasProperty(TYPE) && state.getValue(TYPE) != ChestType.SINGLE) {
            state = state.setValue(TYPE, ChestType.SINGLE);
        }
        return state.setValue(OBSERVER_OPEN, false);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState updatedState = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        if (updatedState == null) return null;
        // We never allow tiered chests to form double-chests.
        if (updatedState.hasProperty(TYPE) && updatedState.getValue(TYPE) != ChestType.SINGLE) {
            updatedState = updatedState.setValue(TYPE, ChestType.SINGLE);
        }
        if (directionToNeighbour == Direction.UP && level instanceof ServerLevel serverLevel && neighbourPos.equals(pos.above())) {
            GlobalTieredChestData.get(serverLevel).onChestAboveChanged(serverLevel, pos);
        }
        return updatedState;
    }
}

