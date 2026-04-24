package com.bobby.bobbychests.capabilities;

import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.blockentity.ModBlockEntities;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.WOODEN_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.COPPER_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.IRON_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.GOLD_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.DIAMOND_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.EMERALD_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.DIRT_CHEST.get(),
                (AbstractTieredChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
    }
}

