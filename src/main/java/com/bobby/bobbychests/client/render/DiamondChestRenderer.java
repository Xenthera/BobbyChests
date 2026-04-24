package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.DiamondChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DiamondChestRenderer extends AbstractChestRenderer<DiamondChestBlockEntity> {
    public DiamondChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "diamond_chest");
    }
}

