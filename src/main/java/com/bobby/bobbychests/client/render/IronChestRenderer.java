package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.IronChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class IronChestRenderer extends AbstractChestRenderer<IronChestBlockEntity> {
    public IronChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "iron_chest");
    }
}

