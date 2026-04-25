package com.bobby.bobbychests.client.render.netherite;

import com.bobby.bobbychests.blockentity.netherite.NetheriteChestBlockEntity;
import com.bobby.bobbychests.client.render.AbstractChestRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class NetheriteChestRenderer extends AbstractChestRenderer<NetheriteChestBlockEntity> {
    public NetheriteChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "netherite_chest");
    }
}
