package com.bobby.bobbychests.capabilities;

import com.bobby.bobbychests.blockentity.BobbyBaseChestBlockEntity;
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
                ModBlockEntities.BOBBY_BASE_CHEST.get(),
                (BobbyBaseChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
    }
}

