package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.WoodenChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class WoodenChestRenderer extends AbstractChestRenderer<WoodenChestBlockEntity> {
    public WoodenChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "wooden_chest");
    }
}

