package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.blockentity.GoldChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class GoldChestRenderer extends AbstractChestRenderer<GoldChestBlockEntity> {
    public GoldChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "gold_chest");
    }
}

