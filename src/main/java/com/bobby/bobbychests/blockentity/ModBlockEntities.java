package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BobbyChests.MODID);

    public static final Supplier<BlockEntityType<BobbyBaseChestBlockEntity>> BOBBY_BASE_CHEST = BLOCK_ENTITIES.register(
                    "bobby_base_chest",
                    () -> new BlockEntityType<>(
                            BobbyBaseChestBlockEntity::new,
                            false,
                            ModBlocks.BOBBY_BASE_CHEST.get()
                    )
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

}
