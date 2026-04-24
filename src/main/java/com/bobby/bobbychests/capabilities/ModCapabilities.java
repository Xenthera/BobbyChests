package com.bobby.bobbychests.capabilities;

import com.bobby.bobbychests.blockentity.FirstChestBlockEntity;
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
                ModBlockEntities.FIRST_CHEST.get(),
                (FirstChestBlockEntity chest, @Nullable Direction side) -> chest.getItemResourceHandler(side)
        );
    }
}

