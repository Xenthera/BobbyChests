package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.DirtChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DirtChestRenderer extends AbstractChestRenderer<DirtChestBlockEntity> {
    public DirtChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "dirt_chest");
    }
}

