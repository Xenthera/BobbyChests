package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.CopperChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class CopperChestRenderer extends AbstractChestRenderer<CopperChestBlockEntity> {
    public CopperChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "copper_chest");
    }
}

