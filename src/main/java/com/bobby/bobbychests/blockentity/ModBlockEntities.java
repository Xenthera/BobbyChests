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

    public static final Supplier<BlockEntityType<FirstChestBlockEntity>> FIRST_CHEST = BLOCK_ENTITIES.register(
                    "first_chest",
                    () -> new BlockEntityType<>(
                            FirstChestBlockEntity::new,
                            false,
                            ModBlocks.FIRST_CHEST.get()
                    )
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

}
