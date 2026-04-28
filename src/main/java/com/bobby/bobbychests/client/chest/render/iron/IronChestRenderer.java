package com.bobby.bobbychests.client.chest.render.iron;

import com.bobby.bobbychests.client.chest.render.AbstractChestRenderer;

import com.bobby.bobbychests.chest.blockentity.iron.IronChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class IronChestRenderer extends AbstractChestRenderer<IronChestBlockEntity> {
    public IronChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "iron_chest");
    }
}

