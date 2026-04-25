package com.bobby.bobbychests.client.render.gold;

import com.bobby.bobbychests.client.render.AbstractChestRenderer;

import com.bobby.bobbychests.blockentity.gold.GoldChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class GoldChestRenderer extends AbstractChestRenderer<GoldChestBlockEntity> {
    public GoldChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "gold_chest");
    }
}

