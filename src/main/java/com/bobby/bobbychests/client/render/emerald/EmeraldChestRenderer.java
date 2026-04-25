package com.bobby.bobbychests.client.render.emerald;

import com.bobby.bobbychests.client.render.AbstractChestRenderer;

import com.bobby.bobbychests.blockentity.emerald.EmeraldChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class EmeraldChestRenderer extends AbstractChestRenderer<EmeraldChestBlockEntity> {
    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "emerald_chest");
    }
}
