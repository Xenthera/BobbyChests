package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.EmeraldChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class EmeraldChestRenderer extends AbstractChestRenderer<EmeraldChestBlockEntity> {
    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "emerald_chest");
    }
}
