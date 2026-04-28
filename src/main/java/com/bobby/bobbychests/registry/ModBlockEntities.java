package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.registry.ModBlocks;
import com.bobby.bobbychests.chest.blockentity.copper.CopperChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.diamond.DiamondChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.dirt.DirtChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.emerald.EmeraldChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.gold.GoldChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.iron.IronChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.netherite.NetheriteChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.wooden.WoodenChestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BobbyChests.MODID);

    public static final Supplier<BlockEntityType<WoodenChestBlockEntity>> WOODEN_CHEST = BLOCK_ENTITIES.register(
            "wooden_chest",
            () -> new BlockEntityType<>(
                    WoodenChestBlockEntity::new,
                    false,
                    ModBlocks.WOODEN_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<CopperChestBlockEntity>> COPPER_CHEST = BLOCK_ENTITIES.register(
            "copper_chest",
            () -> new BlockEntityType<>(
                    CopperChestBlockEntity::new,
                    false,
                    ModBlocks.COPPER_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<IronChestBlockEntity>> IRON_CHEST = BLOCK_ENTITIES.register(
            "iron_chest",
            () -> new BlockEntityType<>(
                    IronChestBlockEntity::new,
                    false,
                    ModBlocks.IRON_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<GoldChestBlockEntity>> GOLD_CHEST = BLOCK_ENTITIES.register(
            "gold_chest",
            () -> new BlockEntityType<>(
                    GoldChestBlockEntity::new,
                    false,
                    ModBlocks.GOLD_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<DiamondChestBlockEntity>> DIAMOND_CHEST = BLOCK_ENTITIES.register(
            "diamond_chest",
            () -> new BlockEntityType<>(
                    DiamondChestBlockEntity::new,
                    false,
                    ModBlocks.DIAMOND_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<EmeraldChestBlockEntity>> EMERALD_CHEST = BLOCK_ENTITIES.register(
            "emerald_chest",
            () -> new BlockEntityType<>(
                    EmeraldChestBlockEntity::new,
                    false,
                    ModBlocks.EMERALD_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<NetheriteChestBlockEntity>> NETHERITE_CHEST = BLOCK_ENTITIES.register(
            "netherite_chest",
            () -> new BlockEntityType<>(
                    NetheriteChestBlockEntity::new,
                    false,
                    ModBlocks.NETHERITE_CHEST.get()
            )
    );

    public static final Supplier<BlockEntityType<DirtChestBlockEntity>> DIRT_CHEST = BLOCK_ENTITIES.register(
            "dirt_chest",
            () -> new BlockEntityType<>(
                    DirtChestBlockEntity::new,
                    false,
                    ModBlocks.DIRT_CHEST.get()
            )
    );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

}
