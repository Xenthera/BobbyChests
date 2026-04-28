package com.bobby.bobbychests.client.chest.render.copper;

import com.bobby.bobbychests.client.chest.render.AbstractChestRenderer;

import com.bobby.bobbychests.chest.blockentity.copper.CopperChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class CopperChestRenderer extends AbstractChestRenderer<CopperChestBlockEntity> {
    public CopperChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "copper_chest");
    }
}

