package com.bobby.bobbychests.client.render.diamond;

import com.bobby.bobbychests.client.render.AbstractChestRenderer;

import com.bobby.bobbychests.blockentity.diamond.DiamondChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DiamondChestRenderer extends AbstractChestRenderer<DiamondChestBlockEntity> {
    public DiamondChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "diamond_chest");
    }
}

