package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.compat.computercraft.ComputerCraftCompat;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class ModCapabilities {

    /**
     * Every tiered chest block entity type. All of them get the same three capabilities, so they are
     * listed once here rather than repeating a registration block per tier.
     */
    private static final List<Supplier<? extends BlockEntityType<? extends AbstractTieredChestBlockEntity>>> CHEST_TYPES = List.of(
            ModBlockEntities.WOODEN_CHEST,
            ModBlockEntities.COPPER_CHEST,
            ModBlockEntities.IRON_CHEST,
            ModBlockEntities.GOLD_CHEST,
            ModBlockEntities.DIAMOND_CHEST,
            ModBlockEntities.EMERALD_CHEST,
            ModBlockEntities.NETHERITE_CHEST,
            ModBlockEntities.DIRT_CHEST
    );

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // Each accessor returns null unless the chest is in the matching resource mode, so an item
        // chest genuinely has no fluid capability for a pipe to find, and vice versa.
        for (Supplier<? extends BlockEntityType<? extends AbstractTieredChestBlockEntity>> type : CHEST_TYPES) {
            registerAll(event, type.get());
        }

        if (ModList.get().isLoaded("computercraft")) {
            BobbyChests.LOGGER.info("Hello computercraft 😘");
            ComputerCraftCompat.register(event);
        }
    }

    private static <T extends AbstractTieredChestBlockEntity> void registerAll(
            RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                type,
                (T chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                type,
                (T chest, @Nullable Direction side) -> chest.getFluidResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                type,
                (T chest, @Nullable Direction side) -> chest.getEnergyHandler(side)
        );
    }
}
