package com.bobby.bobbychests.client.chest.render.emerald;

import com.bobby.bobbychests.client.chest.render.AbstractChestRenderer;

import com.bobby.bobbychests.chest.blockentity.emerald.EmeraldChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class EmeraldChestRenderer extends AbstractChestRenderer<EmeraldChestBlockEntity> {
    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "emerald_chest");
    }
}
