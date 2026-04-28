package com.bobby.bobbychests.client.chest.render.diamond;

import com.bobby.bobbychests.client.chest.render.AbstractChestRenderer;

import com.bobby.bobbychests.chest.blockentity.diamond.DiamondChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DiamondChestRenderer extends AbstractChestRenderer<DiamondChestBlockEntity> {
    public DiamondChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "diamond_chest");
    }
}

