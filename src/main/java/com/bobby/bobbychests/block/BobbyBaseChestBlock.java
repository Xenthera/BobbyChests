package com.bobby.bobbychests.block;

import com.bobby.bobbychests.blockentity.BobbyBaseChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.globalcheststorage.GlobalBobbyBaseChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalBobbyBaseChestData;
import com.bobby.bobbychests.menu.BobbyBaseChestMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BobbyBaseChestBlock extends ChestBlock {
    /**
     * Hidden state used to force a blockstate change on open/close so observers can detect it.
     * This does not affect rendering (both variants map to the same model in the blockstate json).
     */
    public static final BooleanProperty OBSERVER_OPEN = BooleanProperty.create("observer_open");

    public BobbyBaseChestBlock(Properties properties) {
        super(ModBlockEntities.BOBBY_BASE_CHEST::get, SoundEvents.ENDER_CHEST_OPEN, SoundEvents.ENDER_CHEST_CLOSE, properties.sound(SoundType.COPPER).lightLevel(state -> 7));
        this.registerDefaultState(this.defaultBlockState()
                .setValue(TYPE, ChestType.SINGLE)
                .setValue(OBSERVER_OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OBSERVER_OPEN);
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BobbyBaseChestBlockEntity(pos, state);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {

        if(super.getMenuProvider(state, level, pos) == null) return null; // Whatever logic happening in vanilla here prevents using if chest is blocked from above.

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BobbyBaseChestBlockEntity chest)) return null;

        ChestType type = state.getValue(ChestBlock.TYPE);
        Component title = type == ChestType.SINGLE
                ? Component.translatable("container.bobbychests.bobby_base_chest")
                : Component.translatable("container.bobbychests.bobby_base_chest_double");

        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        GlobalBobbyBaseChestData global = GlobalBobbyBaseChestData.get(serverLevel);
        GlobalBobbyBaseChestContainer globalContainer = new GlobalBobbyBaseChestContainer(global, chest);
        return new BobbyBaseChestMenuProvider(title, globalContainer, pos, chest);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            BlockEntity be = serverLevel.getBlockEntity(pos);
            if (be instanceof BobbyBaseChestBlockEntity chest) {
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
            if (be instanceof BobbyBaseChestBlockEntity chest) {
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
        if(state == null) return null;
        return state.setValue(TYPE, ChestType.SINGLE).setValue(OBSERVER_OPEN, false);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState updatedState = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        if(updatedState == null) return null;
        if(updatedState.hasProperty(TYPE) && updatedState.getValue(TYPE) != ChestType.SINGLE){
            updatedState = updatedState.setValue(TYPE, ChestType.SINGLE);
        }
        if (directionToNeighbour == Direction.UP && level instanceof ServerLevel serverLevel && neighbourPos.equals(pos.above())) {
            GlobalBobbyBaseChestData.get(serverLevel).onChestAboveChanged(serverLevel, pos);
        }
        return updatedState;
    }
}
