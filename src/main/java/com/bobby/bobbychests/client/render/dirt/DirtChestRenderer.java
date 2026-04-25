package com.bobby.bobbychests.client.render.dirt;

import com.bobby.bobbychests.client.render.AbstractChestRenderer;

import com.bobby.bobbychests.blockentity.dirt.DirtChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DirtChestRenderer extends AbstractChestRenderer<DirtChestBlockEntity> {
    public DirtChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "dirt_chest");
    }
}

