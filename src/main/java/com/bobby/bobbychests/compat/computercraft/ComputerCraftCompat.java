package com.bobby.bobbychests.compat.computercraft;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Supplier;

public final class ComputerCraftCompat {
    private ComputerCraftCompat() {}

    public static void register(RegisterCapabilitiesEvent event) {
        registerChestPeripheral(event, ModBlockEntities.WOODEN_CHEST);
        registerChestPeripheral(event, ModBlockEntities.COPPER_CHEST);
        registerChestPeripheral(event, ModBlockEntities.IRON_CHEST);
        registerChestPeripheral(event, ModBlockEntities.GOLD_CHEST);
        registerChestPeripheral(event, ModBlockEntities.DIAMOND_CHEST);
        registerChestPeripheral(event, ModBlockEntities.EMERALD_CHEST);
        registerChestPeripheral(event, ModBlockEntities.NETHERITE_CHEST);
        registerChestPeripheral(event, ModBlockEntities.DIRT_CHEST);
    }

    private static <T extends AbstractTieredChestBlockEntity> void registerChestPeripheral(
            RegisterCapabilitiesEvent event,
            Supplier<BlockEntityType<T>> blockEntityType
    ) {
        event.registerBlockEntity(
                PeripheralCapability.get(),
                blockEntityType.get(),
                (T chest, Direction direction) -> new TieredChestPeripheral(chest, direction)
        );
    }
}
