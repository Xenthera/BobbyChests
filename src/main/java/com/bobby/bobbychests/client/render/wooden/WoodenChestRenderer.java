package com.bobby.bobbychests.client.render.wooden;

import com.bobby.bobbychests.client.render.AbstractChestRenderer;

import com.bobby.bobbychests.blockentity.wooden.WoodenChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class WoodenChestRenderer extends AbstractChestRenderer<WoodenChestBlockEntity> {
    public WoodenChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "wooden_chest");
    }
}

